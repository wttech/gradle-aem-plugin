package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.base.vlt.VltFilter
import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.internal.DependencyOptions
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.pkg.PackageFileFilter
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.util.regex.Pattern
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jsoup.nodes.Element

open class Compose : Zip(), AemTask {

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
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default.
     */
    @Input
    var bundlePath: String = aem.config.packageInstallPath

    /**
     * Dependent OSGi bundles to be resolved from repositories and put into CRX package being built.
     */
    @Internal
    val bundleFiles = project.configurations.create(BUNDLE_FILES_CONFIGURATION) { it.isTransitive = false }

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
    var vaultProperties: MutableMap<String, Any> = VAULT_PROPERTIES_DEFAULT.toMutableMap()

    fun vaultProperties(properties: Map<String, Any>) = vaultProperties.putAll(properties)

    fun vaultProperty(name: String, value: String) = vaultProperties(mapOf(name to value))

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     * Must be absolute or relative to current working directory.
     */
    @Input
    var vaultExtraPath: String = project.rootProject.file("aem/${PackagePlugin.VLT_PATH}").toString()

    /**
     * CRX package Vault files will be composed from given sources.
     * Missing files required by package within installation will be auto-generated if 'vaultCopyMissingFiles' is enabled.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultFilesDirs: List<File>
        get() {
            val paths = listOf(vaultExtraPath, "$contentPath/${PackagePlugin.VLT_PATH}")

            return paths.asSequence()
                    .filter { !it.isBlank() }
                    .map { File(it) }
                    .filter { it.exists() }
                    .toList()
        }

    @get:Internal
    @get:JsonIgnore
    val vaultPath: String
        get() = "$contentPath/${PackagePlugin.VLT_PATH}"

    @get:Internal
    @get:JsonIgnore
    val vaultFilterPath: String
        get() = "$vaultPath/filter.xml"

    @get:Internal
    @get:JsonIgnore
    val vaultNodeTypesPath: String
        get() = "$vaultPath/nodetypes.cnd"

    @Internal
    val vaultDir = AemTask.temporaryDir(project, name, PackagePlugin.VLT_PATH)

    @Nested
    val fileFilter = PackageFileFilter(project)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) {
        fileFilter.apply(configurer)
    }

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it, fileProperties) }

    @get:Internal
    val fileProperties
        get() = mapOf("compose" to this)

    /**
     * Name visible in CRX package manager
     */
    @Input
    var vaultName: String = ""

    /**
     * Group for categorizing in CRX package manager
     */
    @Input
    var vaultGroup: String = ""

    /**
     * Version visible in CRX package manager.
     */
    @Input
    var vaultVersion: String = ""

    @get:Internal
    val vaultFilters = mutableSetOf<Element>()

    @Internal
    var vaultFilterDefault = { other: Compose -> "<filter root=\"${other.bundlePath}\"/>" }

    @Internal
    val vaultNodeTypesLibs = mutableSetOf<String>()

    @Internal
    val vaultNodeTypesLines = mutableListOf<String>()

    @Internal
    var fromConvention = true

    private var fromProjects = mutableListOf<() -> Unit>()

    private var fromTasks = mutableListOf<() -> Unit>()

    init {
        description = "Composes CRX package from JCR content and built OSGi bundles"
        group = AemTask.GROUP

        baseName = aem.baseName
        duplicatesStrategy = DuplicatesStrategy.WARN
        isZip64 = true

        doLast { aem.notifier.notify("Package composed", archiveName) }
    }

    override fun projectEvaluated() {
        if (vaultGroup.isBlank()) {
            vaultGroup = if (project == project.rootProject) {
                project.group.toString()
            } else {
                project.rootProject.name
            }
        }
        if (vaultName.isBlank()) {
            vaultName = baseName
        }
        if (vaultVersion.isBlank()) {
            vaultVersion = project.version.toString()
        }

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
        inputs.files(bundleFiles)
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
            fileFilterDelegate(spec)
        }
    }

    fun fromProject(project: Project) { // TODO options closure (bundleRunMode, bundlePath, content = true, bundles = false etc)
        fromProjects.add {
            val other by lazy { AemExtension.of(project) }

            if (project.plugins.hasPlugin(PackagePlugin.ID)) {
                fromCompose(other.compose)
            }

            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                fromBundle(other.bundle)
            }
        }
    }

    fun fromCompose(composeTaskPath: String) {
        fromCompose(project.tasks.getByPath(composeTaskPath) as Compose)
    }

    fun fromCompose(other: Compose) {
        fromTasks.add {
            if (this@Compose != other) {
                dependsOn(other.dependsOn)
            }

            fromJarsInternal(other.bundleFiles.resolve(), other.bundlePath)

            extractVaultFilters(other)
            extractVaultNodeTypes(other)

            val contentDir = File("${other.contentPath}/${PackagePlugin.JCR_ROOT}")
            if (contentDir.exists()) {
                into(PackagePlugin.JCR_ROOT) { spec ->
                    spec.from(contentDir)
                    fileFilterDelegate(spec)
                }
            }

            val hooksDir = File("${other.contentPath}/${PackagePlugin.VLT_HOOKS_PATH}")
            if (hooksDir.exists()) {
                into(PackagePlugin.VLT_HOOKS_PATH) { spec ->
                    spec.from(hooksDir)
                    fileFilterDelegate(spec)
                }
            }
        }
    }

    fun fromBundle(bundle: BundleJar) = fromJar(bundle.jar, bundle.installPath)

    fun fromJar(dependencyNotation: Any) {
        project.dependencies.add(BUNDLE_FILES_CONFIGURATION, dependencyNotation)
    }

    fun fromJar(dependencyOptions: DependencyOptions.() -> Unit) {
        fromJar(DependencyOptions.of(project.dependencies, dependencyOptions))
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

    private fun fromJarsInternal(bundles: Collection<File>, bundlePath: String? = null) {
        if (bundles.isNotEmpty()) {
            into("${PackagePlugin.JCR_ROOT}/${bundlePath ?: this.bundlePath}") { spec ->
                spec.from(bundles)
                fileFilterDelegate(spec)
            }
        }
    }

    private fun extractVaultFilters(other: Compose) {
        if (!other.vaultFilterPath.isBlank() && File(other.vaultFilterPath).exists()) {
            vaultFilters.addAll(VltFilter(File(other.vaultFilterPath)).rootElements)
        } else if (project.plugins.hasPlugin(BundlePlugin.ID)) {
            vaultFilters.add(VltFilter.rootElement(vaultFilterDefault(other)))
        }
    }

    private fun extractVaultNodeTypes(other: Compose) {
        if (other.vaultNodeTypesPath.isBlank()) {
            return
        }

        val file = File(other.vaultNodeTypesPath)
        if (file.exists()) {
            file.forEachLine { line ->
                if (NODE_TYPES_LIB.matcher(line.trim()).matches()) {
                    vaultNodeTypesLibs += line
                } else {
                    vaultNodeTypesLines += line
                }
            }
        }
    }

    companion object {
        const val NAME = "aemCompose"

        const val BUNDLE_FILES_CONFIGURATION = "bundleConfiguration"

        val VAULT_PROPERTIES_DEFAULT = mapOf(
                "acHandling" to "merge_preserve",
                "requiresRoot" to false
        )

        val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")
    }
}