package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
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
        super.copy()
    }

    private fun fromBundles() {
        bundleCollectors.onEach { it() }
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

    // TODO preserve order of inclusion in 'settings.xml' (does filter root order matter?)
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

    val expandPredefinedProperties: Map<String, Any>
        get() {

            return mapOf(
                    "rootProject" to project.rootProject,
                    "project" to project,
                    "config" to config,
                    "created" to ISO8601Utils.format(config.buildDate),
                    "buildCount" to SimpleDateFormat("yDDmmssSSS").format(config.buildDate),
                    "filterRoots" to parseVaultFilterRoots()
            )
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

    fun includeSubprojects() {
        includeProjects("${project.path}:*")
    }

    fun includeProjects(pathFilter: String) {
        project.gradle.afterProject { subproject ->
            if (subproject.path != project.path
                    && subproject.plugins.hasPlugin(AemPlugin.ID)
                    && (pathFilter.isNullOrBlank() || FilenameUtils.wildcardMatch(subproject.path, pathFilter, IOCase.INSENSITIVE))) {
                includeProject(subproject)
            }
        }
    }

    fun includeProjects(projectPaths: Collection<String>) {
        projectPaths.onEach { includeProject(it) }
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

            if (jars.isNotEmpty()) {
                into("${AemPlugin.JCR_ROOT}/$installPath") { spec ->
                    spec.from(jars)
                    config.fileFilter(this, spec)
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
            if (contentDir.exists()) {
                into(AemPlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    config.fileFilter(this, spec)
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
            into(AemPlugin.VLT_PATH, {
                spec -> spec.from(vltPath)
                config.fileFilter(this, spec)
            })
        }
    }
}
