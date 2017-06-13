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
import java.text.SimpleDateFormat
import java.util.*

open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"
    }

    @Internal
    var bundleCollectors: MutableMap<String, MutableList<() -> Set<File>>> = mutableMapOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    // TODO parse filter.xml from includeContent() calls and put lines here (use jsoup to retrieve them?)
    @Internal
    private val includedFilterRoots = mutableListOf<String>()

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
        for ((installPath, jarCollectors) in bundleCollectors) {
            val jars = jarCollectors.fold(TreeSet<File>(), { files, it -> files.addAll(it()); files })
            if (jars.isEmpty()) {
                logger.info("No bundles to copy into AEM package at install path '$installPath'")
            } else {
                logger.info("Copying bundles into AEM package at install path '$installPath': " + jars.toString())
                into("${AemPlugin.JCR_ROOT}/$installPath") { spec -> spec.from(jars) }
            }
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

        val now = Date()

        val template = SimpleTemplateEngine().createTemplate(interpolated).make(mapOf(
                "rootProject" to project.rootProject,
                "project" to project,
                "config" to config,
                "created" to ISO8601Utils.format(now),
                "buildCount" to SimpleDateFormat("yDDmmssSSS").format(now),
                "includedFilterRoots" to includedFilterRoots.joinToString(config.vaultLineSeparator)
        ))

        return template.toString()
    }

    private fun fromContents() {
        contentCollectors.onEach { it() }
    }

    fun includeSubprojects() {
        includeSubprojects(true)
    }

    fun includeSubprojects(withSamePathPrefix: Boolean) {
        project.gradle.afterProject { subproject ->
            if (subproject.path != project.path && subproject.plugins.hasPlugin(AemPlugin.ID)) {
                if (!withSamePathPrefix || subproject.path.startsWith("${project.path}:")) {
                    includeProject(subproject)
                }
            }
        }
    }

    fun includeProjects(vararg projects: Project) {
        includeProjects(projects.toSet())
    }

    fun includeProjects(projects: Collection<Project>) {
        projects.forEach { includeProject(it) }
    }

    fun includeProject(projectPath: String) {
        includeProject(project.findProject(projectPath))
    }

    fun includeProject(project: Project) {
        includeContent(project)
        includeBundles(project, config.bundlePath)
    }

    fun includeBundles(projectPath: String) {
        includeBundles(project.findProject(projectPath), config.bundlePath)
    }

    fun includeBundles(projectPath: String, installPath: String) {
        includeBundles(project.findProject(projectPath), installPath)
    }

    fun includeBundlesAtRunMode(projectPath: String, runMode: String) {
        val project = project.findProject(projectPath)
        includeBundles(project, "${config.bundlePath}.$runMode")
    }

    fun includeBundles(project: Project, installPath: String) {
        dependProject(project, config.dependBundlesTaskNames(project))

        bundleCollectors.getOrPut(installPath, { mutableListOf() }).add({
            JarCollector(project).all.toSet()
        })
    }

    fun includeContent(projectPath: String) {
        includeContent(project.findProject(projectPath))
    }

    fun includeContent(project: Project) {
        dependProject(project, config.dependContentTaskNames(project))

        // TODO copy filter lines and assemble them into one aggregated file using var ${includedFilterRoots}
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

    fun dependProject(projectPath: String, taskNames: Set<String>) {
        dependProject(project.findProject(projectPath), taskNames)
    }

    fun dependProject(project: Project, taskNames: Set<String>) {
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
