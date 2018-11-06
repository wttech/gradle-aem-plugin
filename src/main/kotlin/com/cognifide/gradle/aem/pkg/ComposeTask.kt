package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.file.FileContentReader
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jsoup.nodes.Element
import java.io.File
import java.io.Serializable
import java.util.regex.Pattern
import aQute.bnd.osgi.Jar as OsgiJar

open class ComposeTask : Zip(), AemTask {

    @Nested
    final override val aem = AemExtension.of(project)

    /**
     * Absolute path to JCR content to be included in CRX package.
     *
     * Must be absolute or relative to current working directory.
     */
    @Input
    var contentPath: String = aem.config.packageRoot

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
     * Exclude files being a part of CRX package.
     */
    @Input
    var packageExcludeFiles: MutableList<String> = mutableListOf(
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
     * Wildcard file name filter expression that is used to filter in which Vault files properties can be injected.
     */
    @Input
    var packageExpandFiles: MutableList<String> = mutableListOf(
            "**/${PackagePlugin.VLT_PATH}/*.xml",
            "**/${PackagePlugin.VLT_PATH}/nodetypes.cnd"
    )

    /**
     * Define here custom properties that can be used in CRX package files like 'META-INF/vault/properties.xml'.
     * Could override predefined properties provided by plugin itself.
     */
    @Input
    var packageExpandProperties: MutableMap<String, Any> = mutableMapOf()

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var vaultCopyMissingFiles: Boolean = true

    /**
     * Additional entries added to file 'META-INF/vault/properties.xml'.
     */
    @Input
    var vaultProperties: MutableMap<String, Any> = mutableMapOf(
            "acHandling" to "merge_preserve",
            "requiresRoot" to false
    )

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
     * CRX package Vault node types path.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultNodeTypesPath: String
        get() = "$vaultPath/nodetypes.cnd"

    @Internal
    val vaultDir = AemTask.temporaryDir(project, name, PackagePlugin.VLT_PATH)

    @Nested
    val fileFilterOptions = FileFilterOptions()

    @Internal
    var fileFilter: ((CopySpec) -> Unit) = { spec ->
        if (fileFilterOptions.excluding) {
            spec.exclude(packageExcludeFiles)
        }

        spec.eachFile { fileDetail ->
            val path = "/${fileDetail.relativePath.pathString.removePrefix("/")}"

            if (fileFilterOptions.expanding) {
                if (Patterns.wildcard(path, packageExpandFiles)) {
                    FileContentReader.filter(fileDetail) { aem.props.expandPackage(it, fileProperties, path) }
                }
            }

            if (fileFilterOptions.bundleChecking) {
                if (Patterns.wildcard(path, "**/install/*.jar")) {
                    val bundle = fileDetail.file
                    val isBundle = try {
                        val manifest = OsgiJar(bundle).manifest.mainAttributes
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

    @Internal
    val filters = mutableSetOf<Element>()

    @Internal
    var filterDefault = { other: ComposeTask -> "<filter root=\"${other.bundlePath}\"/>" }

    @Internal
    val nodeTypesLibs = mutableSetOf<String>()

    @Internal
    val nodeTypesLines = mutableListOf<String>()

    @get:Internal
    val fileProperties
        get() = mapOf(
                "compose" to this,
                "filters" to filters,
                "nodeTypesLibs" to nodeTypesLibs,
                "nodeTypesLines" to nodeTypesLines
        ) + packageExpandProperties

    @Internal
    var fromConvention = true

    @Internal
    private var fromProjects = mutableListOf<() -> Unit>()

    @Internal
    private var fromTasks = mutableListOf<() -> Unit>()

    init {
        description = "Composes CRX package from JCR content and built OSGi bundles"
        group = AemTask.GROUP

        baseName = aem.config.baseName
        duplicatesStrategy = DuplicatesStrategy.WARN
        isZip64 = true

        doLast { aem.notifier.notify("Package composed", archiveName) }
    }

    override fun projectEvaluated() {
        if (contentPath.isBlank()) {
            throw AemException("Content path cannot be blank")
        }

        if (bundlePath.isBlank()) {
            throw AemException("Bundle path cannot be blank")
        }

        if (fromConvention) {
            fromConvention()
        }
    }

    override fun projectsEvaluated() {
        vaultFilesDirs.forEach { dir -> inputs.dir(dir) }
        fromProjects.forEach { it() }
        fromTasks.forEach { it() }
    }

    @TaskAction
    override fun copy() {
        copyVaultFiles()
        super.copy()
    }

    private fun copyVaultFiles() {
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

        if (vaultCopyMissingFiles) {
            FileOperations.copyResources(PackagePlugin.VLT_PATH, vaultDir, true)
        }
    }

    fun fromConvention() {
        fromVault()
        fromProject()
    }

    fun fromProject(path: String) = fromProject(project.project(path))

    fun fromProject() = fromProject(project)

    fun fromProjects(pathFilter: String) {
        project.allprojects
                .filter { Patterns.wildcard(it.path, pathFilter) }
                .forEach { fromProject(it) }
    }

    fun fromSubProjects() {
        if (project == project.rootProject) {
            fromProjects(":*")
        } else {
            fromProjects("${project.path}:*")
        }
    }

    fun fromVault() = fromVault(vaultDir)

    fun fromVault(vaultDir: File) {
        into(PackagePlugin.VLT_PATH) { spec ->
            spec.from(vaultDir)
            fileFilter(spec)
        }
    }

    fun fromProject(project: Project) {
        fromProjects.add {
            if (!project.plugins.hasPlugin(PackagePlugin.ID)) {
                return@add
            }

            project.tasks.findByName(NAME)?.apply {
                fromCompose(this as ComposeTask)
            }

            project.tasks.findByName(JavaPlugin.JAR_TASK_NAME)?.apply {
                fromJar(this as Jar, bundlePath)
                fromJars(project.configurations.getByName(BundlePlugin.CONFIG_INSTALL).resolve(), bundlePath)
            }
        }
    }

    fun fromCompose(composeTaskPath: String) {
        fromCompose(project.tasks.getByPath(composeTaskPath) as ComposeTask)
    }

    fun fromCompose(other: ComposeTask) {
        fromTasks.add {
            if (this@ComposeTask != other) {
                dependsOn(other)
            }

            extractVaultFilters(other)
            extractNodeTypes(other)

            val contentDir = File("${other.contentPath}/${PackagePlugin.JCR_ROOT}")
            if (contentDir.exists()) {
                into(PackagePlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    fileFilter(spec)
                }
            }

            val hooksDir = File("${other.contentPath}/${PackagePlugin.VLT_HOOKS_PATH}")
            if (hooksDir.exists()) {
                into(PackagePlugin.VLT_HOOKS_PATH) { spec ->
                    spec.from(hooksDir)
                    fileFilter(spec)
                }
            }
        }
    }

    fun fromJar(bundle: Jar, bundlePath: String? = null) {
        fromTasks.add {
            dependsOn(bundle)
            fromJarsInternal(listOf(bundle.archivePath), bundlePath)
        }
    }

    fun fromJar(bundle: File, bundlePath: String? = null) = fromJars(listOf(bundle), bundlePath)

    fun fromJars(bundles: Collection<File>, bundlePath: String? = null) {
        fromTasks.add { fromJarsInternal(bundles, bundlePath) }
    }

    private fun fromJarsInternal(bundles: Collection<File>, bundlePath: String?) {
        if (bundles.isNotEmpty()) {
            into("${PackagePlugin.JCR_ROOT}/${bundlePath ?: this.bundlePath}") { spec ->
                spec.from(bundles)
                fileFilter(spec)
            }
        }
    }

    private fun extractVaultFilters(other: ComposeTask) {
        if (!other.vaultFilterPath.isBlank() && File(other.vaultFilterPath).exists()) {
            filters.addAll(VltFilter(File(other.vaultFilterPath)).rootElements)
        } else if (project.plugins.hasPlugin(BundlePlugin.ID)) {
            filters.add(VltFilter.rootElement(filterDefault(other)))
        }
    }

    private fun extractNodeTypes(other: ComposeTask) {
        if (other.vaultNodeTypesPath.isBlank()) {
            return
        }

        val file = File(other.vaultNodeTypesPath)
        if (file.exists()) {
            file.forEachLine {
                if (NODE_TYPES_LIB.matcher(it.trim()).matches()) {
                    nodeTypesLibs += it
                } else {
                    nodeTypesLines += it
                }
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

        private val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")
    }
}