package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import com.fasterxml.jackson.databind.util.ISO8601Utils
import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import java.io.FileOutputStream
import java.util.*

open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"
    }

    @Internal
    var bundleCollectors: List<() -> Pair<String, List<File>>> = mutableListOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    @OutputDirectory
    private val vaultDir = File(project.buildDir, "$NAME/${AemPlugin.VLT_PATH}")

    @Input
    final override val config = AemConfig.extend(project)

    init {
        description = "Composes AEM package from JCR content and built OSGi bundles"
        group = AemPlugin.TASK_GROUP

        duplicatesStrategy = DuplicatesStrategy.WARN

        // After this project configured
        project.afterEvaluate({
            includeProject(project)
            includeVaultFiles()
        })

        // After all projects configured
        project.gradle.projectsEvaluated({
            fromBundles()
            fromContents()
        })
    }

    @TaskAction
    override fun copy() {
        copyContentVaultFiles()
        copyMissingVaultFiles()
        expandVaultFiles()
        super.copy()
    }

    private fun fromBundles() {
        bundleCollectors.forEach { t -> copyJarsToTarget(t) }
    }

    private fun copyJarsToTarget(t: (() -> Pair<String, List<File>>)) {
        val pair = t()
        val target = pair.first
        val jars = pair.second
        if (!jars.isEmpty()) {
            logger.info("Copying bundles into AEM package: " + jars.toString() + " to " + target)
            into("${AemPlugin.JCR_ROOT}/$target") { spec -> spec.from(jars) }
        }
    }

    private fun includeVaultFiles() {
        contentCollectors += {
            into(AemPlugin.VLT_PATH, { spec -> spec.from(vaultDir) })
        }
    }

    private fun copyContentVaultFiles() {
        val contentPath: String = if (!config.vaultFilesPath.isNullOrBlank()) {
            config.vaultFilesPath
        } else {
            "${config.determineContentPath(project)}/${AemPlugin.VLT_PATH}"
        }

        val contentDir = File(contentPath)
        if (!contentDir.exists()) {
            logger.info("Vault files directory does not exist. Generated defaults will be used.")
        }

        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }

        if (contentDir.exists()) {
            FileUtils.copyDirectory(contentDir, vaultDir)
        }
    }

    private fun copyMissingVaultFiles() {
        if (!config.vaultCopyMissingFiles) {
            return
        }

        for (resourcePath in Reflections(AemPlugin.VLT_PATH, ResourcesScanner()).getResources { true }) {
            val outputFile = File(vaultDir, resourcePath.substringAfterLast("${AemPlugin.VLT_PATH}/"))
            if (!outputFile.exists()) {
                val input = javaClass.getResourceAsStream("/" + resourcePath)
                val output = FileOutputStream(outputFile)

                try {
                    IOUtils.copy(input, output)
                } finally {
                    IOUtils.closeQuietly(input)
                    IOUtils.closeQuietly(output)
                }
            }
        }
    }

    private fun expandVaultFiles() {
        val files = vaultDir.listFiles { _, name -> config.vaultFilesExpanded.any { FilenameUtils.wildcardMatch(name, it, IOCase.INSENSITIVE) } } ?: return

        for (file in files) {
            val content = try {
                expandProperties(file.inputStream().bufferedReader().use { it.readText() })
            } catch (e: Exception) {
                throw PackageException("Cannot expand Vault files properly. Probably some variables are not bound", e)
            }

            file.printWriter().use { it.print(content) }
        }
    }

    private fun expandProperties(source: String): String {
        val props = System.getProperties().entries.fold(mutableMapOf<String, String>(), { map, entry ->
            map.put(entry.key.toString(), entry.value.toString()); map
        }) + config.vaultExpandProperties
        val interpolated = StrSubstitutor.replace(source, props)
        val template = SimpleTemplateEngine().createTemplate(interpolated).make(mapOf(
                "rootProject" to project.rootProject,
                "project" to project,
                "config" to config,
                "created" to ISO8601Utils.format(Date())
        ))

        return template.toString()
    }

    private fun fromContents() {
        contentCollectors.onEach { it() }
    }

    fun includeProject(projectPath: String) {
        includeProject(project.findProject(projectPath))
    }

    fun includeProject(project: Project) {
        includeContent(project)
        includeBundles(project)
    }

    fun includeBundles(projectPath: String) {
        includeBundles(project.findProject(projectPath), config.dependBundlesTaskNames(project))
    }

    fun includeBundles(projectPath: String, taskNames: Set<String>) {
        includeBundles(project.findProject(projectPath), taskNames)
    }

    fun includeBundles(project: Project) {
        includeBundles(project, config.dependBundlesTaskNames(project))
    }

    fun includeBundles(project: Project, taskNames: Set<String>) {
        dependProject(project, taskNames)

        val extraProperties = project.extensions.extraProperties
        val bundlePath: String = if (extraProperties == null || !extraProperties.has("bundlePath")) config.bundlePath
        else extraProperties.get("bundlePath") as String

        bundleCollectors += {
            Pair(bundlePath, JarCollector(project).all.toList())
        }
    }

    fun includeContent(projectPath: String) {
        includeContent(project.findProject(projectPath))
    }

    fun includeContent(projectPath: String, taskNames : Set<String>) {
        includeContent(project.findProject(projectPath), taskNames)
    }

    fun includeContent(project: Project) {
        includeContent(project, config.dependContentTaskNames(project))
    }

    fun includeContent(project: Project, taskNames : Set<String>) {
        dependProject(project, taskNames)

        contentCollectors += {
            val contentDir = File("${config.determineContentPath(project)}/${AemPlugin.JCR_ROOT}")
            if (!contentDir.exists()) {
                logger.info("Package JCR content directory does not exist: ${contentDir.absolutePath}")
            } else {
                logger.info("Copying JCR content from: ${contentDir.absolutePath}")

                into(AemPlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    exclude(config.contentFileIgnores)
                }
            }
        }
    }

    fun dependProject(project: Project, taskNames : Set<String>) {
        taskNames.forEach { taskName -> dependsOn("${project.path}:$taskName") }
    }

    fun includeVault(vltPath: Any) {
        into(AemPlugin.VLT_PATH, { spec -> spec.from(vltPath) })
    }

    fun includeVaultProfile(profileName: String) {
        includeVault(project.relativePath(config.vaultCommonPath))
        includeVault(project.relativePath(config.vaultProfilePath + "/" + profileName))
    }
}
