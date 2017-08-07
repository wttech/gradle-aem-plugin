package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemBasePlugin
import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPackagePlugin
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File

open class ComposeTask : Zip(), AemTask {

    companion object {
        val NAME = "aemCompose"

        val DEPENDENCIES_SUFFIX = ".dependencies"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @InputFiles
    val vaultFilters = mutableListOf<File>()

    @InputDirectory
    val vaultDir = AemTask.temporaryDir(project, PrepareTask.NAME, AemPackagePlugin.VLT_PATH)

    @Internal
    private var bundleCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private var contentCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private val filterRoots = mutableSetOf<String>()

    @Internal
    private val propertyParser = PropertyParser(project)

    private var archiveName: String? = null

    @Internal
    var fileFilter: ((CopySpec) -> Unit) = { spec ->
        spec.exclude(config.filesExcluded)
        spec.eachFile({ fileDetail ->
            if (Patterns.wildcard(fileDetail.file, config.filesExpanded)) {
                fileDetail.filter({ line -> propertyParser.expand(line, fileProperties) })
            }
        })
    }

    @get:Internal
    val fileProperties
        get() = mapOf("filterRoots" to filterRoots.joinToString(config.vaultLineSeparator))

    init {
        description = "Composes CRX package from JCR content and built OSGi bundles"
        group = AemTask.GROUP

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

    private fun fromBundles() {
        bundleCollectors.onEach { it() }
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
                    && subproject.plugins.hasPlugin(AemBasePlugin.ID)
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

        includeBundlesAtPath(project, AemConfig.of(project).bundlePath)
    }

    fun includeBundles(project: Project) {
        includeBundlesAtPath(project, AemConfig.of(project).bundlePath)
    }

    fun includeBundles(projectPath: String, runMode: String) {
        val project = project.findProject(projectPath)
        val bundlePath = AemConfig.of(project).bundlePath

        includeBundlesAtPath(project, "$bundlePath.$runMode")
    }

    fun mergeBundles(projectPath: String) {
        includeBundlesAtPath(project.findProject(projectPath), config.bundlePath)
    }

    fun mergeBundles(projectPath: String, runMode: String) {
        includeBundlesAtPath(project.findProject(projectPath), "${config.bundlePath}.$runMode")
    }

    fun includeBundlesAtPath(projectPath: String, installPath: String) {
        includeBundlesAtPath(project.findProject(projectPath), installPath)
    }

    fun includeBundlesAtPath(project: Project, installPath: String) {
        val config = AemConfig.of(project)

        dependProject(project, config.dependBundlesTaskNames)

        bundleCollectors += {
            val jars = JarCollector(project).all.toSet()

            if (jars.isNotEmpty()) {
                into("${AemPackagePlugin.JCR_ROOT}/$installPath") { spec ->
                    spec.from(jars)
                    fileFilter(spec)
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
        extractVaultFilters(config)

        contentCollectors += {
            val contentDir = File("${config.contentPath}/${AemPackagePlugin.JCR_ROOT}")
            if (contentDir.exists()) {
                into(AemPackagePlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    fileFilter(spec)
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

    private fun extractVaultFilters(config: AemConfig) {
        if (!config.vaultFilterPath.isNullOrBlank() && File(config.vaultFilterPath).exists()) {
            filterRoots.addAll(extractVaultFilters(File(config.vaultFilterPath)))
        } else {
            filterRoots.add("<filter root=\"${config.bundlePath}\"/>")
        }
    }

    private fun extractVaultFilters(filter: File): Set<String> {
        val doc = Jsoup.parse(filter.bufferedReader().use { it.readText() }, "", Parser.xmlParser())

        return doc.select("filter[root]").map { it.toString() }.toSet()
    }

    fun includeVault(vltPath: Any) {
        contentCollectors += {
            into(AemPackagePlugin.VLT_PATH, { spec ->
                spec.from(vltPath)
                fileFilter(spec)
            })
        }
    }

    @get:Internal
    val defaultArchiveName: String
        get() = super.getArchiveName()

    @get:Internal
    val extendedArchiveName: String
        get() {
            return if (project == project.rootProject || project.name == project.rootProject.name) {
                defaultArchiveName
            } else {
                "${propertyParser.namePrefix}-$defaultArchiveName"
            }
        }

    override fun setArchiveName(name: String?) {
        this.archiveName = name
    }

    override fun getArchiveName(): String {
        if (archiveName != null) {
            return archiveName!!
        }

        return extendedArchiveName
    }
}
