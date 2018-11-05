package com.cognifide.gradle.aem.pkg

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.bundle.BundleCollector
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.file.FileContentReader
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Zip
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jsoup.nodes.Element
import java.io.File
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

open class ComposeTask : Zip(), AemTask {

    @Nested
    final override val aem = AemExtension.of(project)

    @Internal
    val vaultDir = AemTask.temporaryDir(project, name, PackagePlugin.VLT_PATH)

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var vaultCopyMissingFiles: Boolean = true

    @Internal
    val filterRoots = mutableSetOf<Element>()

    @get:Input
    val filterRootsProp: String
        get() = filterRoots.joinToString(aem.config.vaultLineSeparatorString) { it.toString() }

    @Internal
    var filterRootDefault = { other: ComposeTask -> "<filter root=\"${other.bundlePath}\"/>" }

    @Internal
    private var bundleCollectors: List<() -> Unit> = mutableListOf()

    @Internal
    private var contentCollectors: List<() -> Unit> = mutableListOf()

    @Nested
    val fileFilterOptions = FileFilterOptions()

    @Internal
    var fileFilter: ((CopySpec) -> Unit) = { spec ->
        if (fileFilterOptions.excluding) {
            spec.exclude(packageFilesExcluded)
        }

        spec.eachFile { fileDetail ->
            val path = "/${fileDetail.relativePath.pathString.removePrefix("/")}"

            if (fileFilterOptions.expanding) {
                if (Patterns.wildcard(path, packageFilesExpanded)) {
                    FileContentReader.filter(fileDetail) { aem.props.expandPackage(it, fileProperties, path) }
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
                "filterRoots" to filterRootsProp,
                "buildCount" to SimpleDateFormat("yDDmmssSSS").format(packageBuildDate),
                "created" to Formats.date(packageBuildDate)
        ) + packageFileProperties

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
     * Absolute path to JCR content to be included in CRX package.
     *
     * Must be absolute or relative to current working directory.
     */
    @Input
    var contentPath: String = "${project.file("src/main/content")}"

    /**
     * Content path for bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default.
     */
    @Input
    var bundlePath: String = if (project == project.rootProject) {
        "/apps/${project.rootProject.name}/install"
    } else {
        "/apps/${project.rootProject.name}/${aem.config.projectName}/install"
    }

    /**
     * Additional entries added to file 'META-INF/vault/properties.xml'.
     */
    @Input
    var packageEntries: MutableMap<String, Any> = mutableMapOf(
            "acHandling" to "merge_preserve",
            "requiresRoot" to false
    )

    /**
     * Wildcard file name filter expression that is used to filter in which Vault files properties can be injected.
     */
    @Input
    var packageFilesExpanded: MutableList<String> = mutableListOf("**/${PackagePlugin.VLT_PATH}/*.xml")

    /**
     * Define here custom properties that can be used in CRX package files like 'META-INF/vault/properties.xml'.
     * Could override predefined properties provided by plugin itself.
     */
    @Input
    var packageFileProperties: MutableMap<String, Any> = mutableMapOf()

    /**
     * Exclude files being a part of CRX package.
     */
    @Input
    var packageFilesExcluded: MutableList<String> = mutableListOf(
            "**/.gradle",
            "**/.git",
            "**/.git/**",
            "**/.gitattributes",
            "**/.gitignore",
            "**/.gitmodules",
            "**/.vlt",
            "**/.vlt*.tmp",
            "**/node_modules/**",
            "jcr_root/.vlt-sync-config.properties"
    )

    /**
     * Build date used as base for calculating 'created' and 'buildCount' package properties.
     */
    @Internal
    var packageBuildDate: Date = aem.props.date("aem.package.buildDate", Date())


    /**
     * CRX package Vault files will be composed from given sources.
     * Missing files required by package within installation will be auto-generated if 'vaultCopyMissingFiles' is enabled.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultFilesDirs: List<File>
        get() {
            val paths = listOf(
                    aem.config.vaultFilesPath,
                    "$contentPath/${PackagePlugin.VLT_PATH}"
            )

            return paths.asSequence()
                    .filter { !it.isBlank() }
                    .map { File(it) }
                    .filter { it.exists() }
                    .toList()
        }

    /**
     * CRX package Vault files path.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultPath: String
        get() = "$contentPath/${PackagePlugin.VLT_PATH}"

    /**
     * CRX package Vault filter path.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultFilterPath: String
        get() = "$vaultPath/filter.xml"

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

        baseName = aem.config.baseName
        duplicatesStrategy = DuplicatesStrategy.WARN
        isZip64 = true

        // Include itself by default
        includeProject(project)
        includeVault(vaultDir)

        doLast { aem.notifier.notify("Package composed", archiveName) }
    }

    override fun projectEvaluated() {
        if (contentPath.isBlank()) {
            throw AemException("Content path cannot be blank")
        }

        if (bundlePath.isBlank()) {
            throw AemException("Bundle path cannot be blank")
        }
    }

    override fun projectsEvaluated() {
        vaultFilesDirs.forEach { dir -> inputs.dir(dir) }
        bundleCollectors.onEach { it() }
        contentCollectors.onEach { it() }
    }

    @TaskAction
    override fun copy() {
        copyContentVaultFiles()
        copyMissingVaultFiles()
        super.copy()
    }

    private fun copyContentVaultFiles() {
        if (vaultDir.exists()) {
            vaultDir.deleteRecursively()
        }
        vaultDir.mkdirs()

        val dirs = vaultFilesDirs

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
        if (!vaultCopyMissingFiles) {
            return
        }

        FileOperations.copyResources(PackagePlugin.VLT_PATH, vaultDir, true)
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

    fun composeOfProject(project: Project): ComposeTask {
        return project.tasks.getByName(ComposeTask.NAME) as ComposeTask
    }


    fun includeBundlesAtPath(project: Project, installPath: String? = null, runMode: String? = null) {
        bundleCollectors += {
            val other = project.tasks.getByName(ComposeTask.NAME) as ComposeTask // TODO more explicit

            dependProject(project, dependBundlesTaskNames)

            var effectiveInstallPath = if (!installPath.isNullOrBlank()) {
                installPath
            } else {
                other.bundlePath
            }

            if (!runMode.isNullOrBlank()) {
                effectiveInstallPath = "$effectiveInstallPath.$runMode"
            }

            val bundles = BundleCollector(project).allJars  // TODO include jar task ; or File/Configuration
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
            val other = composeOfProject(project)

            dependProject(project, dependContentTaskNames)
            extractVaultFilters(other)

            val contentDir = File("${other.contentPath}/${PackagePlugin.JCR_ROOT}")
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
                val taskName = name.substringBeforeLast(DEPENDENCIES_SUFFIX)
                if (taskName != this.name && project != this.project) {
                    val task = project.tasks.getByName(taskName)
                    val dependencies = task.taskDependencies.getDependencies(task).map { it.name }

                    names.addAll(dependencies)
                }
            } else {
                names.add(name)
            }

            names
        }

        effectiveTaskNames.forEach { taskName ->
            dependsOn("${project.path}:$taskName")
        }
    }

    private fun extractVaultFilters(other: ComposeTask) {
        if (!other.vaultFilterPath.isBlank() && File(other.vaultFilterPath).exists()) {
            filterRoots.addAll(VltFilter(File(other.vaultFilterPath)).rootElements)
        } else if (project.plugins.hasPlugin(BundlePlugin.ID)) {
            filterRoots.add(VltFilter.rootElement(filterRootDefault(other)))
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

    fun fileFilterOptions(configurer: FileFilterOptions.() -> Unit) {
        fileFilterOptions.apply(configurer)
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