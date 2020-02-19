package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.aem.common.pkg.PackageFileFilter
import com.cognifide.gradle.aem.common.pkg.PackageValidator
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.compose.BundleDependency
import com.cognifide.gradle.aem.pkg.tasks.compose.PackageDependency
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.tasks.ZipTask
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.utils.using
import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*

@Suppress("TooManyFunctions", "LargeClass")
open class PackageCompose : ZipTask(), AemTask {

    final override val aem = project.aem

    /**
     * Shorthand for built CRX package file.
     */
    @get:Internal
    val composedFile: File get() = archiveFile.get().asFile

    /**
     * Shorthand for directory of built CRX package file.
     */
    @get:Internal
    val composedDir: File get() = composedFile.parentFile

    /**
     * Absolute path to JCR content to be included in CRX package.
     *
     * Must be absolute or relative to current working directory.
     */
    @Internal
    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     */
    @Input
    val bundlePath = aem.obj.string { convention(aem.packageOptions.installPath) }

    /**
     * Content path for CRX sub-packages being placed in CRX package being built.
     */
    @Internal
    val packagePath = aem.obj.string { convention(aem.packageOptions.storagePath) }

    @Nested
    var validator = PackageValidator(aem).apply {
        workDir.convention(destinationDirectory.dir(Package.OAKPAL_OPEAR_PATH))
    }

    fun validator(options: PackageValidator.() -> Unit) {
        validator.apply(options)
    }

    @InputDirectory
    val metaDir = aem.obj.relativeDir(contentDir, Package.META_PATH)

    /**
     * Defines properties being used to generate CRX package metadata files.
     */
    @Nested
    val vaultDefinition = VaultDefinition(aem)

    fun vaultDefinition(options: VaultDefinition.() -> Unit) {
        vaultDefinition.apply(options)
    }

    fun vaultDefinition(options: Action<in VaultDefinition>) = options.execute(vaultDefinition)

    @Internal
    val vaultDir = aem.obj.relativeDir(contentDir, Package.VLT_PATH)

    @Internal
    val vaultHooksDir = aem.obj.relativeDir(contentDir, Package.VLT_HOOKS_PATH)

    @Internal
    val vaultFilterOriginFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterFile = aem.obj.relativeFile(vaultDir, FilterFile.BUILD_NAME)

    @Internal
    val vaultNodeTypesFile = aem.obj.relativeFile(vaultDir, Package.VLT_NODETYPES_FILE)

    @Internal
    val vaultNodeTypesSyncFile = aem.obj.file { convention(aem.packageOptions.nodeTypesSyncFile) }

    @Nested
    val fileFilter = PackageFileFilter(aem)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) = fileFilter.using(configurer)

    fun fileFilter(configurer: Action<in PackageFileFilter>) = configurer.execute(fileFilter)

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it, vaultDefinition.fileProperties) }

    private var fromProjects = mutableListOf<() -> Unit>()

    private var fromTasks = mutableListOf<() -> Unit>()

    @Input
    val bundleDependencies = aem.obj.list<BundleDependency> { convention(listOf()) }

    @Input
    val packageDependencies = aem.obj.list<PackageDependency> { convention(listOf()) }

    override fun projectsEvaluated() {
        fromProjects.forEach { it() }
        fromTasks.forEach { it() }
    }

    @TaskAction
    override fun copy() {
        super.copy()
        validator.perform(composedFile)

        common.notifier.notify("Package composed", composedFile.name)
    }

    fun fromProject(path: String) = fromProject(project.project(path))

    fun fromProjects(pathFilter: String) {
        project.allprojects
                .filter { Patterns.wildcard(it.path, pathFilter) }
                .forEach { fromProject(it) }
    }

    fun fromSubprojects() {
        if (project == project.rootProject) {
            fromProjects(":*")
        } else {
            fromProjects("${project.path}:*")
        }
    }

    fun fromMeta() = fromMeta(metaDir)

    fun fromMeta(metaDir: Any) {
        into(Package.META_PATH) { spec ->
            spec.from(metaDir)
            fileFilterDelegate(spec)
        }
    }

    fun fromProject(other: Project) {
        fromProjects.add { composeProject(other) }
    }

    fun fromContent(dir: Any) {
        into(Package.JCR_ROOT) { spec ->
            spec.from(dir)
            fileFilterDelegate(spec)
        }
    }

    fun fromVaultHooks(dir: Any) {
        into(Package.VLT_HOOKS_PATH) { spec ->
            spec.from(dir)
            fileFilterDelegate(spec)
        }
    }

    private fun fromPackage() {
        composeSelf()
    }

    fun fromPackage(other: PackageCompose) {
        fromTasks.add { composeOther(other) }
    }

    fun fromBundle(other: BundleCompose) {
        fromTasks.add { composeBundle(other) }
    }

    // TODO support project path only somehow
    /*

    //    fun fromPackage(composeTaskPath: String) {
//        fromPackage(common.tasks.get(composeTaskPath, PackageCompose::class.java),))
//    }

    fun fromBundle(composeTaskPath: String) {
        fromBundle(common.tasks.get(composeTaskPath, BundleCompose::class.java), ProjectMergingOptions())
    }

    private fun fromBundle(bundle: BundleCompose, options: ProjectMergingOptions) {
        if (options.bundleBuilt) {
            fromJar(bundle, Package.bundlePath(bundle.installPath.get(), bundle.installRunMode.orNull), bundle.vaultFilter.get())
        }
    }

    @JvmOverloads
    fun fromJar(dependencyNotation: Any, installPath: String? = null, vaultFilter: Boolean? = null) {
        bundleDependencies.add(BundleDependency(aem,
                project.dependencies.create(dependencyNotation),
                installPath ?: this.bundlePath.get(),
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromJar(jar: Jar, bundlePath: String? = null, vaultFilter: Boolean? = null) {
        fromTasks.add {
            dependsOn(jar)
            fromJarInternal(jar.archiveFile.get().asFile, bundlePath, vaultFilter)
        }
    }

    fun fromJar(jar: File, bundlePath: String? = null, vaultFilter: Boolean? = null) {
        fromJars(listOf(jar), bundlePath, vaultFilter)
    }

    fun fromJars(jars: Collection<File>, bundlePath: String? = null, vaultFilter: Boolean? = null) {
        fromTasks.add {
            jars.forEach { fromJarInternal(it, bundlePath, vaultFilter) }
        }
    }

    private fun fromJarInternal(jar: File, bundlePath: String? = null, vaultFilter: Boolean? = null) {
        val effectiveBundlePath = bundlePath ?: this.bundlePath.get()

        fromArchiveInternal(vaultFilter, effectiveBundlePath, jar)
    }

    @JvmOverloads
    fun fromZip(dependencyNotation: String, storagePath: String? = null, vaultFilter: Boolean? = null) {
        fromZip(StringUtils.appendIfMissing(dependencyNotation, "@zip") as Any, storagePath, vaultFilter)
    }

    fun fromZip(dependencyNotation: Any, storagePath: String? = null, vaultFilter: Boolean? = null) {
        packageDependencies.add(PackageDependency(aem, project.dependencies.create(dependencyNotation),
                storagePath ?: packagePath.get(),
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromZip(zip: File, packagePath: String? = null, vaultFilter: Boolean? = null) {
        fromZips(listOf(zip), packagePath, vaultFilter)
    }

    fun fromZips(zips: Collection<File>, packagePath: String? = null, vaultFilter: Boolean? = null) {
        fromTasks.add {
            zips.forEach { fromZipInternal(it, packagePath, vaultFilter) }
        }
    }

    // TODO support project path only somehow
    fun fromSubpackage(composeTaskPath: String, storagePath: String? = null, vaultFilter: Boolean? = null) {
        fromTasks.add {
            val other = common.tasks.pathed(composeTaskPath).get() as PackageCompose

            dependsOn(other)

            val file = other.composedFile
            val effectivePath = "${storagePath ?: this.packagePath.get()}/${other.vaultDefinition.group.get()}"

            if (vaultFilter ?: mergingOptions.vaultFilters) {
                vaultDefinition.filter("$effectivePath/${file.name}") { type = FilterType.FILE }
            }

            into("${Package.JCR_ROOT}/$effectivePath") { spec ->
                spec.from(other.composedFile)
                fileFilterDelegate(spec)
            }
        }
    }

    private fun fromZipInternal(file: File, packagePath: String? = null, vaultFilter: Boolean? = null) {
        val effectivePackageDir = aem.packageOptions.storageDir(PackageFile(file))
        val effectivePackagePath = "${packagePath ?: this.packagePath.get()}/$effectivePackageDir"

        fromArchiveInternal(vaultFilter, effectivePackagePath, file)
    }

    private fun fromArchiveInternal(vaultFilter: Boolean?, effectivePath: String, file: File) {
        if (vaultFilter ?: mergingOptions.vaultFilters) {
            vaultDefinition.filter("$effectivePath/${file.name}") { type = FilterType.FILE }
        }

        into("${Package.JCR_ROOT}/$effectivePath") { spec ->
            spec.from(file)
            fileFilterDelegate(spec)
        }
    }
     */

    private var composeProject: PackageCompose.(Project) -> Unit = { other ->
        if (other.plugins.hasPlugin(PackagePlugin.ID)) {
            fromPackage(other.common.tasks.get(NAME))
        }

        if (other.plugins.hasPlugin(BundlePlugin.ID)) {
            fromBundle(other.common.tasks.get(BundleCompose.NAME))
        }

        dependsOn(other.common.tasks.checks)
    }

    fun composeProject(action: PackageCompose.(other: Project) -> Unit) {
        this.composeProject = action
    }

    private var composeSelf: PackageCompose.() -> Unit = {
        fromMeta()
        fromContent(contentDir)
        fromVaultHooks(vaultHooksDir)

        vaultDefinition.filters(vaultFilterOriginFile)
        vaultDefinition.nodeTypes(vaultNodeTypesSyncFile)
    }

    fun composeSelf(action: PackageCompose.() -> Unit) {
        this.composeSelf = action
    }

    private var composeOther: PackageCompose.(PackageCompose) -> Unit = { other ->
        if (this@PackageCompose == other) {
            throw PackageException("Package cannot be composed due to configuration error (circular reference)!")
        }

        dependsOn(other.dependsOn)

        fromContent(other.contentDir)
        fromVaultHooks(other.vaultHooksDir)

        vaultDefinition.filters(other.vaultFilterFile)
        vaultDefinition.nodeTypes(other.vaultNodeTypesFile)
        vaultDefinition.properties.putAll(other.vaultDefinition.properties) // TODO putIfAbsent if possible

        bundleDependencies.addAll(other.bundleDependencies)
        packageDependencies.addAll(other.packageDependencies)
    }

    fun composeOther(action: PackageCompose.(PackageCompose) -> Unit) {
        this.composeOther = action
    }

    private var composeBundle: PackageCompose.(BundleCompose) -> Unit = { other ->
        fromBundleBuilt(other)
    }

    fun fromBundleBuilt(task: BundleCompose) {
        bundleDependencies.add(aem.obj.provider { BundleDependency(
                project.dependencies.create(task.archiveFile),
                Package.bundlePath(task.installPath.get(), task.installRunMode.orNull),
                task.vaultFilter.get())
        })
    }

    fun composeBundle(action: PackageCompose.(other: BundleCompose) -> Unit) {
        this.composeBundle = action
    }

    /**
     * Configures extra files to be observed in case of Gradle task caching.
     *
     * TODO https://github.com/gradle/gradle/issues/2016
     */
    @get:InputFiles
    val inputFiles = aem.obj.files {
        from(vaultNodeTypesSyncFile)
    }

    init {
        description = "Composes CRX package from JCR content and built OSGi bundles"
        archiveBaseName.set(aem.commonOptions.baseName)
        destinationDirectory.set(project.layout.buildDirectory.dir(name))
        duplicatesStrategy = DuplicatesStrategy.WARN
    }

    companion object {
        const val NAME = "packageCompose"
    }
}
