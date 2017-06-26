package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPlugin
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Zip
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import java.io.FileOutputStream

open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"

        val DEPENDENCIES_SUFFIX = ".dependencies"
    }

    @Internal
    var bundleCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    var contentCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private val vaultFilters = mutableListOf<File>()

    @OutputDirectory
    private val vaultDir = File(project.buildDir, "$NAME/${AemPlugin.VLT_PATH}")

    @Nested
    final override val config = AemConfig.of(project)

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

    private fun copyContentVaultFiles() {
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        vaultDir.mkdirs()

        val dirs = config.vaultFilesDirs.filter { it.exists() }

        if (dirs.isEmpty()) {
            logger.info("None of Vault files directories exist: $dirs. Only generated defaults will be used.")
        } else {
            dirs.onEach { dir ->
                logger.info("Copying Vault files from path: '${dir.absolutePath}'")

                FileUtils.copyDirectory(dir, vaultDir)
            }
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
        val files = vaultDir.listFiles { _, name -> config.vaultFilesExpanded.any { Patterns.wildcard(name, it) } } ?: return

        for (file in files) {
            val expandedContent = try {
                expandProperties( file.inputStream().bufferedReader().use { it.readText() })
            } catch (e: Exception) {
                throw PackageException("Cannot expand Vault files properly. Probably some variables are not bound", e)
            }

            file.printWriter().use { it.print(expandedContent) }
        }
    }

    // TODO Preserve order of inclusion in 'settings.xml' (does filter root order matter?)
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

    val fileAllProperties by lazy {
        mapOf("filterRoots" to parseVaultFilterRoots()) + config.fileProperties
    }

    fun expandProperties(fileSource: String): String {
        return expandProperties(fileSource, fileAllProperties)
    }

    fun expandProperties(fileSource: String, props: Map<String, Any>): String {
        return PropertyParser(project).expand(fileSource, props)
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
            if (subproject != project
                    && subproject.plugins.hasPlugin(AemPlugin.ID)
                    && (pathFilter.isNullOrBlank() || Patterns.wildcard(subproject.path, pathFilter))) {
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

    fun includeBundlesAtSamePath(projectPath: String) {
        includeBundles(project.findProject(projectPath), config.bundlePath)
    }

    fun includeBundlesAtSamePath(projectPath: String, runMode: String) {
        includeBundles(project.findProject(projectPath), "${config.bundlePath}.$runMode")
    }

    fun includeBundles(project: Project, installPath: String) {
        val config = AemConfig.of(project)

        dependProject(project, config.dependBundlesTaskNames)

        bundleCollectors += {
            val jars = JarCollector(project).all.toSet()

            if (jars.isNotEmpty()) {
                into("${AemPlugin.JCR_ROOT}/$installPath") { spec ->
                    spec.from(jars)
                    config.fileFilter(spec, this)
                }
            }
        }
    }

    fun includeContent(projectPath: String) {
        includeContent(project.findProject(projectPath))
    }

    fun includeContent(project: Project) {
        val config = AemConfig.of(project)

        dependProject(project, config.dependContentTaskNames)

        if (this.project != project && !config.vaultFilterPath.isNullOrBlank()) {
            vaultFilters.add(File(config.vaultFilterPath))
        }

        contentCollectors += {
            val contentDir = File("${config.contentPath}/${AemPlugin.JCR_ROOT}")
            if (contentDir.exists()) {
                into(AemPlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    config.fileFilter(spec, this)
                }
            }
        }
    }

    fun dependProject(projectPath: String, taskNames: Collection<String>) {
        dependProject(project.findProject(projectPath), taskNames)
    }

    fun dependProject(project: Project, taskNames: Collection<String>) {
        val effectiveTaskNames = taskNames.fold(mutableListOf<String>(), { names, name ->
            if (name.endsWith(DEPENDENCIES_SUFFIX)) {
                val task = project.tasks.getByName(name.substringBeforeLast(DEPENDENCIES_SUFFIX))
                val dependencies = task.taskDependencies.getDependencies(task).map { it.name }

                names.addAll(dependencies)
            } else {
                names.add(name)
            }

            names
        })

        effectiveTaskNames.forEach { taskName ->
            dependsOn("${project.path}:$taskName")
        }
    }

    fun includeVault(vltPath: Any) {
        contentCollectors += {
            into(AemPlugin.VLT_PATH, { spec ->
                spec.from(vltPath)
                config.fileFilter(spec, this)
            })
        }
    }
}
