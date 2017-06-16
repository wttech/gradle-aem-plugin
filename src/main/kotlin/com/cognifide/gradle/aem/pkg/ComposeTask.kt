package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.fasterxml.jackson.databind.util.ISO8601Utils
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
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
    var bundleCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private val vaultFilters = mutableListOf<File>()

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
            includeVault(vaultDir)
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
        bundleCollectors.onEach { it() }
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
            "${config.contentPath}/${AemPlugin.VLT_PATH}"
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

    private fun parseVaultFilterRoots(): String {
        val tags = vaultFilters.filter { it.exists() }.fold(mutableListOf<String>(), { tags, filter ->
            val doc = Jsoup.parse(filter.bufferedReader().use { it.readText() }, "", Parser.xmlParser())
            tags.addAll(doc.select("filter[root]").map { it.toString() }.toList()); tags
        })

        if (tags.isEmpty()) {
            tags.add("<filter root=\"${config.bundlePath}\"/>")
        }

        return tags.joinToString(config.vaultLineSeparator)
    }

    private fun expandVaultFiles() {
        val files = vaultDir.listFiles { _, name -> config.vaultFilesExpanded.any { FilenameUtils.wildcardMatch(name, it, IOCase.INSENSITIVE) } } ?: return

        for (file in files) {
            val expandedContent = try {
                val rawContent = file.inputStream().bufferedReader().use { it.readText() }
                PropertyParser(project).expand(rawContent, expandPredefinedProps)
            } catch (e: Exception) {
                throw PackageException("Cannot expand Vault files properly. Probably some variables are not bound", e)
            }

            file.printWriter().use { it.print(expandedContent) }
        }
    }

    private val expandPredefinedProps: Map<String, Any>
        get() {
            val currentDate = Date()

            return mapOf(
                    "rootProject" to project.rootProject,
                    "project" to project,
                    "config" to config,
                    "currentDate" to currentDate,
                    "created" to ISO8601Utils.format(currentDate),
                    "buildCount" to SimpleDateFormat("yDDmmssSSS").format(currentDate),
                    "filterRoots" to parseVaultFilterRoots()
            )
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

    fun includeProject(projectPath: String) {
        includeProject(project.findProject(projectPath))
    }

    fun includeProject(project: Project) {
        includeContent(project)
        includeBundles(project)
    }

    fun includeBundles(projectPath: String) {
        val project = project.findProject(projectPath)

        includeBundles(project, AemConfig.of(project).bundlePath)
    }

    fun includeBundles(project: Project) {
        includeBundles(project, AemConfig.of(project).bundlePath)
    }

    fun includeBundles(projectPath: String, installPath: String) {
        includeBundles(project.findProject(projectPath), installPath)
    }

    fun includeBundlesAtRunMode(projectPath: String, runMode: String) {
        val project = project.findProject(projectPath)
        val bundlePath = AemConfig.of(project).bundlePath

        includeBundles(project, "$bundlePath.$runMode")
    }

    fun includeBundles(project: Project, installPath: String) {
        val config = AemConfig.of(project)

        dependProject(project, config.dependBundlesTaskNames(project))

        bundleCollectors += {
            val jars = JarCollector(project).all.toSet()

            if (jars.isEmpty()) {
                logger.info("No bundles to copy into AEM package at install path '$installPath'")
            } else {
                logger.info("Copying bundles into AEM package at install path '$installPath': " + jars.toString())

                into("${AemPlugin.JCR_ROOT}/$installPath") { spec ->
                    spec.from(jars)
                }
            }
        }
    }

    fun includeContent(projectPath: String) {
        includeContent(project.findProject(projectPath))
    }

    fun includeContent(project: Project) {
        val config = AemConfig.of(project)

        dependProject(project, config.dependContentTaskNames(project))

        if (this.project.path != project.path) {
            vaultFilters.add(project.file(config.vaultFilterPath))
        }

        contentCollectors += {
            val contentDir = File("${config.contentPath}/${AemPlugin.JCR_ROOT}")
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
        contentCollectors += {
            logger.info("Including Vault configuration into CRX package from: '$vltPath'")

            into(AemPlugin.VLT_PATH, { spec -> spec.from(vltPath) })
        }
    }
}
