package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.aem.common.pkg.PackageFileFilter
import com.cognifide.gradle.aem.common.pkg.PackageValidator
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import com.cognifide.gradle.aem.common.pkg.vault.FilterType
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.aem.pkg.tasks.compose.*
import com.cognifide.gradle.common.tasks.ZipTask
import com.cognifide.gradle.common.utils.using
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

@Suppress("TooManyFunctions")
open class PackageCompose : ZipTask(), AemTask {

    final override val aem = project.aem

    /**
     * Shorthand for built CRX package file.
     */
    @get:Internal
    val composedFile get() = archiveFile.get().asFile

    /**
     * Shorthand for directory of built CRX package file.
     */
    @get:Internal
    val composedDir get() = composedFile.parentFile

    @Internal
    val contentDir = aem.obj.dir { convention(aem.packageOptions.contentDir) }

    @Internal
    val jcrRootDir = aem.obj.relativeDir(contentDir, Package.JCR_ROOT)

    @Internal
    val metaDir = aem.obj.relativeDir(contentDir, Package.META_PATH)

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     */
    @Input
    val bundlePath = aem.obj.string { convention(aem.packageOptions.installPath) }

    /**
     * Content path for CRX sub-packages being placed in CRX package being built.
     */
    @Internal
    val nestedPath = aem.obj.string { convention(aem.packageOptions.storagePath) }

    @Nested
    var validator = PackageValidator(aem).apply {
        workDir.convention(destinationDirectory.dir(Package.OAKPAL_OPEAR_PATH))
    }

    fun validator(options: PackageValidator.() -> Unit) {
        validator.apply(options)
    }

    /**
     * Defines properties being used to generate CRX package metadata files.
     */
    @Nested
    val vaultDefinition = VaultDefinition(aem)

    fun vaultDefinition(options: VaultDefinition.() -> Unit) {
        vaultDefinition.apply(options)
    }

    @Internal
    val vaultDir = aem.obj.relativeDir(contentDir, Package.VLT_PATH)

    @Internal
    val vaultHooksDir = aem.obj.relativeDir(contentDir, Package.VLT_HOOKS_PATH)

    @Internal
    val vaultFilterOriginFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterFile = aem.obj.relativeFile(vaultDir, FilterFile.BUILD_NAME)

    @Internal
    val vaultFilters = aem.obj.boolean { convention(true) }

    @Internal
    val vaultNodeTypesFile = aem.obj.relativeFile(vaultDir, Package.VLT_NODETYPES_FILE)

    @Internal
    val vaultNodeTypesSyncFile = aem.obj.file { convention(aem.packageOptions.nodeTypesSyncFile) }

    @Nested
    val bundlesInstalled = aem.obj.list<BundleInstalled> { convention(listOf()) }

    @Nested
    val packagesNested = aem.obj.list<PackageNested> { convention(listOf()) }

    private var definitions = mutableListOf<() -> Unit>()

    override fun projectsEvaluated() {
        super.projectsEvaluated()
        (definitions + definition).forEach { it() }
    }

    @TaskAction
    override fun copy() {
        super.copy()
        validator.perform(composedFile)
        common.notifier.notify("Package composed", composedFile.name)
    }

    fun fromScratch() = noDefaults()

    fun fromMeta(metaDir: Any) {
        into(Package.META_PATH) { spec ->
            spec.from(metaDir)
            fileFilterDelegate(spec)
        }
    }

    fun fromRoot(dir: Any) {
        into(Package.JCR_ROOT) { spec ->
            spec.from(dir)
            fileFilterDelegate(spec)
        }
    }

    fun fromBundlesInstalled(bundles: ListProperty<BundleInstalled>) = bundles.get().forEach { fromArchive(it) }

    fun fromPackagesNested(pkgs: ListProperty<PackageNested>) = pkgs.get().forEach { fromArchive(it) }

    private fun fromArchive(archive: RepositoryArchive) {
        val dirPath = archive.dirPath.map { path ->
            when {
                archive is BundleInstalled && archive.runMode.isPresent -> "$path.${archive.runMode.get()}"
                else -> path
            }
        }

        if (archive.vaultFilter.get()) { // TODO lazy?
            vaultDefinition.filter(aem.obj.provider { "${dirPath.get()}/${archive.fileName.get()}" }) { type = FilterType.FILE }
        }

        into("${Package.JCR_ROOT}/${dirPath.get()}") { spec ->
            spec.from(archive.file)
            fileFilterDelegate(spec)
        }
    }

    fun fromVaultHooks(dir: Any) {
        into(Package.VLT_HOOKS_PATH) { spec ->
            spec.from(dir)
            fileFilterDelegate(spec)
        }
    }

    fun withBundlesInstalled(others: ListProperty<BundleInstalled>) {
        bundlesInstalled.addAll(others)
    }

    fun withPackagesNested(others: ListProperty<PackageNested>) {
        packagesNested.addAll(others)
    }

    fun withVaultFilters(file: RegularFileProperty) {
        vaultDefinition.filters(file)
    }

    fun withVaultNodeTypes(file: RegularFileProperty) {
        vaultDefinition.nodeTypes(file)
    }

    fun withVaultDefinition(other: VaultDefinition) {
        vaultDefinition.properties.putAll(other.properties)
        vaultDefinition.nodeTypeLibs.addAll(other.nodeTypeLibs)
        vaultDefinition.nodeTypeLines.addAll(other.nodeTypeLines)
    }

    fun mergePackageProject(projectPath: String) = mergePackage("$projectPath:$NAME")

    fun mergePackage(taskPath: String) = mergePackage(common.tasks.pathed(taskPath))

    fun mergePackage(task: TaskProvider<PackageCompose>) {
        definitions.add { task.get().merging(this) }
    }

    fun nestPackage(dependencyNotation: Any, options: PackageNestedResolved.() -> Unit = {}) {
        definitions.add { packagesNested.add(PackageNestedResolved(this, dependencyNotation).apply(options)) }
    }

    fun nestPackageProject(projectPath: String, options: PackageNestedBuilt.() -> Unit = {}) {
        nestPackageBuilt("$projectPath:$NAME", options)
    }

    fun nestPackageBuilt(taskPath: String, options: PackageNestedBuilt.() -> Unit = {}) {
        nestPackageBuilt(common.tasks.pathed(taskPath), options)
    }

    fun nestPackageBuilt(task: TaskProvider<PackageCompose>, options: PackageNestedBuilt.() -> Unit = {}) {
        definitions.add {
            dependsOn(task)
            packagesNested.add(PackageNestedBuilt(this, task).apply(options))
        }
    }

    fun installBundle(dependencyNotation: Any, options: BundleInstalledResolved.() -> Unit = {}) {
        definitions.add { bundlesInstalled.add(BundleInstalledResolved(this, dependencyNotation).apply(options)) }
    }

    fun installBundleProject(projectPath: String, options: BundleInstalledBuilt.() -> Unit = {}) {
        installBundleBuilt("$projectPath:${BundleCompose.NAME}", options)
    }

    fun installBundleBuilt(taskPath: String, options: BundleInstalledBuilt.() -> Unit = {}) {
        installBundleBuilt(common.tasks.pathed(taskPath), options)
    }

    fun installBundleBuilt(task: TaskProvider<BundleCompose>, options: BundleInstalledBuilt.() -> Unit = {}) {
        definitions.add {
            dependsOn(task)
            bundlesInstalled.add(BundleInstalledBuilt(this, task).apply(options))
        }
    }

    private var definition: () -> Unit = {
        withVaultFilters(vaultFilterOriginFile)
        withVaultNodeTypes(vaultNodeTypesSyncFile)

        fromMeta(metaDir)
        fromRoot(jcrRootDir)
        fromBundlesInstalled(bundlesInstalled)
        fromPackagesNested(packagesNested)
        fromVaultHooks(vaultHooksDir)
    }

    /**
     * Override default behavior for composing this package.
     */
    fun definition(definition: () -> Unit) {
        this.definition = definition
    }

    /**
     * Clear default behavior for composing this package.
     * After calling this method, particular 'from*()' methods need to be called.
     */
    fun noDefaults() = definition {}

    private var merging: (PackageCompose) -> Unit = { other ->
        if (this == other) {
            throw PackageException("Package cannot be composed due to configuration error (circular reference)!")
        }

        other.dependsOn(dependsOn)

        other.withVaultFilters(vaultFilterFile)
        other.withVaultNodeTypes(vaultNodeTypesFile)
        other.withVaultDefinition(vaultDefinition)
        other.withBundlesInstalled(bundlesInstalled)
        other.withPackagesNested(packagesNested)

        other.fromRoot(jcrRootDir)
        other.fromVaultHooks(vaultHooksDir)
    }

    /**
     * Override default behavior for merging this package into assembly package.
     */
    fun merging(action: (PackageCompose) -> Unit) {
        this.merging = action
    }

    /**
     * Add some extra behavior when merging this package into assembly package.
     */
    fun merged(action: (PackageCompose) -> Unit) {
        val defaultAction = this.merging
        this.merging = { other ->
            defaultAction(other)
            action(other)
        }
    }

    @Nested
    val fileFilter = PackageFileFilter(this)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) = fileFilter.using(configurer)

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it, vaultDefinition.fileProperties) }

    @get:InputFiles // TODO https://github.com/gradle/gradle/issues/2016
    val inputFiles = aem.obj.files { from(vaultNodeTypesSyncFile) }

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
