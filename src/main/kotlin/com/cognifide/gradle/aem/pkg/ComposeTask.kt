package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemPackagePlugin
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
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

    @InputDirectory
    val vaultDir = AemTask.temporaryDir(project, PrepareTask.NAME, AemPackagePlugin.VLT_PATH)

    @Input
    val filterRoots = mutableSetOf<String>()

    @Internal
    val filterRootDefault = { subproject: Project, subconfig: AemConfig ->
        "<filter root=\"${subconfig.bundlePath}\"/>"
    }

    @Internal
    val propertyParser = PropertyParser(project)

    @Internal
    private var bundleCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private var contentCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private var archiveName: String? = null

    @Internal
    var fileFilter: ((CopySpec) -> Unit) = { spec ->
        spec.exclude(config.filesExcluded)
        spec.eachFile({ fileDetail ->
            val file = fileDetail.file
            if (Patterns.wildcard(file, config.filesExpanded)) {
                fileDetail.filter({ line -> propertyParser.expand(line, fileProperties, file.absolutePath) })
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
        isZip64 = true

        // Include itself by default
        includeProject(project)
        includeVault(vaultDir)

        // Evaluate inclusion above and other cross project inclusions
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
                    && subproject.plugins.hasPlugin(AemPackagePlugin.ID)
                    && (pathFilter.isBlank() || Patterns.wildcard(subproject.path, pathFilter))) {
                includeProject(subproject)
            }
        }
    }

    fun includeProjects(projectPaths: Collection<String>) {
        projectPaths.onEach { includeProject(it) }
    }

    fun includeBundles(projectPath: String) {
        val project = project.findProject(projectPath)

        includeBundlesAtPath(project)
    }

    fun includeBundles(project: Project) {
        includeBundlesAtPath(project)
    }

    fun includeBundles(projectPath: String, runMode: String) {
        includeBundlesAtPath(project.findProject(projectPath), runMode = runMode)
    }

    fun mergeBundles(projectPath: String) {
        includeBundlesAtPath(project.findProject(projectPath))
    }

    fun mergeBundles(projectPath: String, runMode: String) {
        includeBundlesAtPath(project.findProject(projectPath), runMode = runMode)
    }

    fun includeBundlesAtPath(projectPath: String, installPath: String) {
        includeBundlesAtPath(project.findProject(projectPath), installPath)
    }

    fun includeBundlesAtPath(project: Project, installPath: String? = null, runMode: String? = null) {
        bundleCollectors += {
            val config = AemConfig.of(project)

            dependProject(project, config.dependBundlesTaskNames)

            var effectiveInstallPath = if (!installPath.isNullOrBlank()) {
                installPath
            } else {
                AemConfig.of(project).bundlePath
            }

            if (!runMode.isNullOrBlank()) {
                effectiveInstallPath = "$effectiveInstallPath.$runMode"
            }

            val jars = JarCollector(project).all.toSet()

            if (jars.isNotEmpty()) {
                into("${AemPackagePlugin.JCR_ROOT}/$effectiveInstallPath") { spec ->
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
        contentCollectors += {
            val config = AemConfig.of(project)

            dependProject(project, config.dependContentTaskNames)
            extractVaultFilters(project, config)

            val contentDir = File("${config.contentPath}/${AemPackagePlugin.JCR_ROOT}")
            if (contentDir.exists()) {
                into(AemPackagePlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    fileFilter(spec)
                }
            }
        }
    }

    private fun dependProject(projectPath: String, taskNames: Collection<String>) {
        dependProject(project.findProject(projectPath), taskNames)
    }

    private fun dependProject(project: Project, taskNames: Collection<String>) {
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

    private fun extractVaultFilters(project: Project, config: AemConfig) {
        if (!config.vaultFilterPath.isNullOrBlank() && File(config.vaultFilterPath).exists()) {
            filterRoots.addAll(extractVaultFilters(File(config.vaultFilterPath)))
        } else {
            filterRoots.add(filterRootDefault(project, config))
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
