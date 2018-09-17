package com.cognifide.gradle.aem.pkg

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemNotifier
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.bundle.BundleCollector
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileContentReader
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.ConfigureUtil
import org.jsoup.nodes.Element
import java.io.File
import java.io.Serializable

open class ComposeTask : Zip(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    @InputDirectory
    val vaultDir = AemTask.temporaryDir(project, PrepareTask.NAME, PackagePlugin.VLT_PATH)

    @Internal
    val filterRoots = mutableSetOf<Element>()

    @get:Input
    val filterRootsProp: String
        get() = filterRoots.joinToString(config.vaultLineSeparatorString) { it.toString() }

    @Internal
    var filterRootDefault = { _: Project, subconfig: AemConfig ->
        "<filter root=\"${subconfig.bundlePath}\"/>"
    }

    @Internal
    val propertyParser = PropertyParser(project)

    @Internal
    private var bundleCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private var contentCollectors: List<() -> Unit> = mutableListOf()

    @Nested
    val fileFilterOptions = FileFilterOptions()

    @Internal
    var fileFilter: ((CopySpec) -> Unit) = { spec ->
        if (fileFilterOptions.excluding) {
            spec.exclude(config.packageFilesExcluded)
        }

        spec.eachFile { fileDetail ->
            val path = "/${fileDetail.relativePath.pathString.removePrefix("/")}"

            if (fileFilterOptions.expanding) {
                if (Patterns.wildcard(path, config.packageFilesExpanded)) {
                    FileContentReader.filter(fileDetail, { propertyParser.expandPackage(it, fileProperties, path) })
                }
            }

            if (fileFilterOptions.bundleChecking) {
                if (Patterns.wildcard(path, "**/install/*.jar")) {
                    val bundle = fileDetail.file
                    val isBundle = try {
                        val manifest = Jar(bundle).manifest.mainAttributes
                        !manifest.getValue("Bundle-SymbolicName").isNullOrBlank()
                    } catch (e: Exception) {
                        false
                    }

                    if (!isBundle) {
                        logger.warn("Jar being a part of composed CRX package is not a valid OSGi bundle: $bundle")
                        fileDetail.exclude()
                    }
                }
            }
        }
    }

    @get:Internal
    val fileProperties
        get() = mapOf(
                "zip" to this,
                "filters" to filterRoots,
                "filterRoots" to filterRootsProp
        )

    /**
     * Configure default task dependency assignments while including dependant project bundles.
     * Simplifies multi-module project configuration.
     */
    @Input
    var dependBundlesTaskNames: List<String> = mutableListOf(
            LifecycleBasePlugin.ASSEMBLE_TASK_NAME,
            LifecycleBasePlugin.CHECK_TASK_NAME
    )

    /**
     * Configure default task dependency assignments while including dependant project content.
     * Simplifies multi-module project configuration.
     */
    @Input
    var dependContentTaskNames: List<String> = mutableListOf(
            ComposeTask.NAME + DEPENDENCIES_SUFFIX
    )

    init {
        description = "Composes CRX package from JCR content and built OSGi bundles"
        group = AemTask.GROUP

        baseName = AemConfig.pkgVaultName(project)
        duplicatesStrategy = DuplicatesStrategy.WARN
        isZip64 = true

        // Include itself by default
        includeProject(project)
        includeVault(vaultDir)

        // Evaluate inclusion above and other cross project inclusions
        project.gradle.projectsEvaluated {
            fromBundles()
            fromContents()
        }

        doLast { AemNotifier.of(project).default("Package composed", getArchiveName()) }
    }

    private fun fromBundles() {
        bundleCollectors.onEach { it() }
    }

    private fun fromContents() {
        contentCollectors.onEach { it() }
    }

    fun includeProject(projectPath: String) {
        includeProject(findProject(projectPath))
    }

    fun includeProject(projectPath: String, runMode: String) {
        includeContent(findProject(projectPath))
        includeBundles(projectPath, runMode)
    }

    fun includeProject(project: Project) {
        includeContent(project)
        includeBundles(project)
    }

    fun includeSubprojects() {
        if (project == project.rootProject) {
            includeProjects(":*")
        } else {
            includeProjects("${project.path}:*")
        }
    }

    fun includeProjects(pathFilter: String) {
        project.gradle.afterProject { subproject ->
            if (subproject != project
                    && subproject.plugins.hasPlugin(PackagePlugin.ID)
                    && (pathFilter.isBlank() || Patterns.wildcard(subproject.path, pathFilter))) {
                includeProject(subproject)
            }
        }
    }

    fun includeProjects(projectPaths: Collection<String>) {
        projectPaths.onEach { includeProject(it) }
    }

    fun includeBundles(projectPath: String) {
        includeBundlesAtPath(findProject(projectPath))
    }

    fun includeBundles(project: Project) {
        includeBundlesAtPath(project)
    }

    fun includeBundles(projectPath: String, runMode: String) {
        includeBundlesAtPath(findProject(projectPath), runMode = runMode)
    }

    fun mergeBundles(projectPath: String) {
        includeBundlesAtPath(findProject(projectPath))
    }

    fun mergeBundles(projectPath: String, runMode: String) {
        includeBundlesAtPath(findProject(projectPath), runMode = runMode)
    }

    fun includeBundlesAtPath(projectPath: String, installPath: String) {
        includeBundlesAtPath(findProject(projectPath), installPath)
    }

    fun includeBundlesAtPath(project: Project, installPath: String? = null, runMode: String? = null) {
        bundleCollectors += {
            val config = AemConfig.of(project)

            dependProject(project, dependBundlesTaskNames)

            var effectiveInstallPath = if (!installPath.isNullOrBlank()) {
                installPath
            } else {
                config.bundlePath
            }

            if (!runMode.isNullOrBlank()) {
                effectiveInstallPath = "$effectiveInstallPath.$runMode"
            }

            val bundles = BundleCollector(project).allJars
            if (bundles.isNotEmpty()) {
                into("${PackagePlugin.JCR_ROOT}/$effectiveInstallPath") { spec ->
                    spec.from(bundles)
                    fileFilter(spec)
                }
            }
        }
    }

    fun includeContent(projectPath: String) {
        includeContent(findProject(projectPath))
    }

    fun includeContent(project: Project) {
        contentCollectors += {
            val config = AemConfig.of(project)

            dependProject(project, dependContentTaskNames)
            extractVaultFilters(project, config)

            val contentDir = File("${config.contentPath}/${PackagePlugin.JCR_ROOT}")
            if (contentDir.exists()) {
                into(PackagePlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    fileFilter(spec)
                }
            }
        }
    }

    private fun findProject(projectPath: String): Project {
        return project.findProject(projectPath)
                ?: throw AemException("Project cannot be found by path '$projectPath'")
    }

    private fun dependProject(projectPath: String, taskNames: Collection<String>) {
        dependProject(findProject(projectPath), taskNames)
    }

    private fun dependProject(project: Project, taskNames: Collection<String>) {
        val effectiveTaskNames = taskNames.fold(mutableListOf<String>()) { names, name ->
            if (name.endsWith(DEPENDENCIES_SUFFIX)) {
                val task = project.tasks.getByName(name.substringBeforeLast(DEPENDENCIES_SUFFIX))
                val dependencies = task.taskDependencies.getDependencies(task).map { it.name }

                names.addAll(dependencies)
            } else {
                names.add(name)
            }

            names
        }

        effectiveTaskNames.forEach { taskName ->
            dependsOn("${project.path}:$taskName")
        }
    }

    private fun extractVaultFilters(project: Project, config: AemConfig) {
        if (!config.vaultFilterPath.isBlank() && File(config.vaultFilterPath).exists()) {
            filterRoots.addAll(VltFilter(File(config.vaultFilterPath)).rootElements)
        } else if (project.plugins.hasPlugin(BundlePlugin.ID)) {
            filterRoots.add(VltFilter.rootElement(filterRootDefault(project, config)))
        }
    }

    fun includeVault(vltPath: Any) {
        contentCollectors += {
            into(PackagePlugin.VLT_PATH) { spec ->
                spec.from(vltPath)
                fileFilter(spec)
            }
        }
    }

    fun fileFilterOptions(closure: Closure<*>) {
        ConfigureUtil.configure(closure, fileFilterOptions)
    }

    class FileFilterOptions : Serializable {

        @Input
        var excluding: Boolean = true

        @Input
        var expanding: Boolean = true

        @Input
        var bundleChecking: Boolean = true

    }

    companion object {
        const val NAME = "aemCompose"

        const val DEPENDENCIES_SUFFIX = ".dependencies"
    }
}