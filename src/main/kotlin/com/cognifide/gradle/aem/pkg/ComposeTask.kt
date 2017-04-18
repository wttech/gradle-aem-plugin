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
import org.gradle.api.plugins.BasePlugin
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

/**
 * TODO Input is also effected by SCR plugin / metadata?
 */
open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"
    }

    @Internal
    var bundleCollectors: List<() -> List<File>> = mutableListOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    @OutputDirectory
    val vaultDir = File(project.buildDir, "${NAME}/${AemPlugin.VLT_PATH}")

    @Input
    override val config = AemConfig.extendFromGlobal(project)

    init {
        description = "Composes AEM / CRX package from JCR content and built JAR bundles."
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

    private fun determineContentPath(project: Project): String {
        val task = project.tasks.getByName(ComposeTask.NAME) as ComposeTask

        return project.projectDir.path + "/" + task.config.contentPath
    }

    private fun fromBundles() {
        val jars = bundleCollectors.fold(TreeSet<File>(), { files, it -> files.addAll(it()); files }).toList()
        if (jars.isEmpty()) {
            logger.info("No bundles to copy into AEM package")
        } else {
            logger.info("Copying bundles into AEM package: " + jars.toString())
            into("${AemPlugin.JCR_ROOT}/${config.bundlePath}") { spec -> spec.from(jars) }
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
            "${determineContentPath(project)}/${AemPlugin.VLT_PATH}"
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
        for (file in vaultDir.listFiles { _, name -> config.vaultFilesExpanded.any { FilenameUtils.wildcardMatch(name, it, IOCase.INSENSITIVE) } }) {
            val content = try {
                expandProperties(file.inputStream().bufferedReader().use { it.readText() })
            } catch (e: Exception) {
                throw PackageException("Cannot fetch vault files properly. Probably some variables are not bound", e)
            }

            file.printWriter().use { it.print(content) }
        }
    }

    private fun expandProperties(source: String): String {
        val props = System.getProperties().entries.fold(mutableMapOf<String, String>(), { map, entry ->
            map.put(entry.key.toString(), entry.value.toString()); map
        }) + config.vaultProperties
        val interpolated = StrSubstitutor.replace(source, props)
        val template = SimpleTemplateEngine().createTemplate(interpolated).make(mapOf(
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
        includeJcrRoot(project)
        includeBundles(project)

        dependsOn("${project.path}:${BasePlugin.ASSEMBLE_TASK_NAME}")
    }

    fun includeBundles(projectPath: String) {
        includeBundles(project.findProject(projectPath))
    }

    fun includeBundles(project: Project) {
        bundleCollectors += {
            JarCollector(project).all.toList()
        }
    }

    fun includeJcrRoot(projectPath: String) {
        includeJcrRoot(project.findProject(projectPath))
    }

    fun includeJcrRoot(project: Project) {
        contentCollectors += {
            val contentDir = File("${determineContentPath(project)}/${AemPlugin.JCR_ROOT}")
            if (!contentDir.exists()) {
                logger.info("Package JCR content directory does not exist: ${contentDir.absolutePath}")
            } else {
                logger.info("Copying JCR content from: ${contentDir.absolutePath}")

                into(AemPlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    exclude(config.fileIgnores)
                }
            }
        }
    }

    fun includeVault(vltPath: Any) {
        into(AemPlugin.VLT_PATH, { spec -> spec.from(vltPath) })
    }

    fun includeVaultProfile(profileName: String) {
        includeVault(project.relativePath(config.vaultCommonPath))
        includeVault(project.relativePath(config.vaultProfilePath + "/" + profileName))
    }
}
