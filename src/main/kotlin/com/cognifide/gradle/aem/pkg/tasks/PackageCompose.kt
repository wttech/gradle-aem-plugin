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
import com.cognifide.gradle.aem.common.pkg.vault.FilterType
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.compose.*
import com.cognifide.gradle.common.common
import com.cognifide.gradle.common.tasks.ZipTask
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.utils.using
import java.io.File
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*

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
    val vaultFilters = aem.obj.boolean { convention(true) }

    @Internal
    val vaultNodeTypesFile = aem.obj.relativeFile(vaultDir, Package.VLT_NODETYPES_FILE)

    @Internal
    val vaultNodeTypesSyncFile = aem.obj.file { convention(aem.packageOptions.nodeTypesSyncFile) }

    @Nested
    val bundlesInstalled = aem.obj.list<BundleInstalled> { convention(listOf()) }

    @Nested
    val packagesNested = aem.obj.list<PackageNested> { convention(listOf()) }

    private var fromProjects = mutableListOf<() -> Unit>()

    private var fromTasks = mutableListOf<() -> Unit>()

    override fun projectsEvaluated() {
        super.projectsEvaluated()
        fromProjects.forEach { it() }
        fromTasks.forEach { it() }
        composeSelf()
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

    fun fromMeta(metaDir: Any) {
        into(Package.META_PATH) { spec ->
            spec.from(metaDir)
            fileFilterDelegate(spec)
        }
    }

    fun fromProject(other: Project) {
        fromProjects.add { composeProject(other) }
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
        if (archive.vaultFilter.get()) { // TODO lazy?
            vaultDefinition.filter(aem.obj.provider { "${archive.dirPath.get()}/${archive.fileName.get()}" }) { type = FilterType.FILE }
        }

        into("${Package.JCR_ROOT}/${archive.dirPath.get()}") { spec ->
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

    fun fromVaultFilters(file: RegularFileProperty) {
        vaultDefinition.filters(file)
    }

    fun fromVaultNodeTypes(file: RegularFileProperty) {
        vaultDefinition.nodeTypes(file)
    }

    fun mergePackage(taskPath: String) = mergePackage(common.tasks.pathed(taskPath))

    fun mergePackage(task: TaskProvider<PackageCompose>) {
        fromTasks.add { task.get().composeOther(this) }
    }

    fun nestPackage(dependencyNotation: Any) {
        fromTasks.add { packagesNested.add(PackageNestedResolved(this, dependencyNotation)) }
    }

    fun nestPackageProject(projectPath: String) = nestPackageBuilt("$projectPath:$NAME")

    fun nestPackageBuilt(taskPath: String) = nestPackageBuilt(common.tasks.pathed(taskPath))

    fun nestPackageBuilt(task: TaskProvider<PackageCompose>) {
        fromTasks.add {
            dependsOn(task)
            packagesNested.add(PackageNestedBuilt(this, task))
        }
    }

    fun installBundle(dependencyNotation: Any) {
        fromTasks.add { bundlesInstalled.add(BundleInstalledResolved(this, dependencyNotation)) }
    }

    fun installBundleProject(projectPath: String) = installBundleBuilt("$projectPath:${BundleCompose.NAME}")

    fun installBundleBuilt(taskPath: String) = installBundleBuilt(common.tasks.pathed(taskPath))

    fun installBundleBuilt(task: TaskProvider<BundleCompose>) {
        fromTasks.add {
            dependsOn(task)
            bundlesInstalled.add(BundleInstalledBuilt(this, task))
        }
    }

    private var composeProject: PackageCompose.(Project) -> Unit = { other ->
        if (other.plugins.hasPlugin(PackagePlugin.ID)) {
            mergePackage(other.common.tasks.named(NAME)) // TODO merge vs nest by default?
        }
        if (other.plugins.hasPlugin(BundlePlugin.ID)) {
            installBundleBuilt(other.common.tasks.named(BundleCompose.NAME))
        }
        dependsOn(other.common.tasks.checks)
    }

    fun composeProject(action: PackageCompose.(other: Project) -> Unit) {
        this.composeProject = action
    }

    private var composeSelf: () -> Unit = {
        fromMeta(metaDir)
        fromRoot(jcrRootDir)
        fromBundlesInstalled(bundlesInstalled)
        fromPackagesNested(packagesNested)
        fromVaultHooks(vaultHooksDir)
        fromVaultFilters(vaultFilterOriginFile)
        fromVaultNodeTypes(vaultNodeTypesSyncFile)
    }

    fun composeSelf(action: () -> Unit) {
        this.composeSelf = action
    }

    private var composeOther: (PackageCompose) -> Unit = { other ->
        if (this == other) {
            throw PackageException("Package cannot be composed due to configuration error (circular reference)!")
        }

        other.dependsOn(dependsOn)

        other.fromRoot(jcrRootDir)
        other.fromVaultHooks(vaultHooksDir)

        other.vaultDefinition.filters(vaultFilterFile)
        other.vaultDefinition.nodeTypes(vaultNodeTypesFile)
        other.vaultDefinition.properties.putAll(vaultDefinition.properties)

        other.bundlesInstalled.addAll(bundlesInstalled)
        other.packagesNested.addAll(packagesNested)
    }

    fun composeOther(action: (PackageCompose) -> Unit) {
        this.composeOther = action
    }

    @Nested
    val fileFilter = PackageFileFilter(aem)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) = fileFilter.using(configurer)

    fun fileFilter(configurer: Action<in PackageFileFilter>) = configurer.execute(fileFilter)

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it, vaultDefinition.fileProperties) }

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
