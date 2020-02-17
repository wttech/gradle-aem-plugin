package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.aem
import com.cognifide.gradle.aem.bundle.tasks.BundleCompose
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.common.pkg.PackageFileFilter
import com.cognifide.gradle.aem.common.pkg.PackageValidator
import com.cognifide.gradle.aem.common.pkg.vault.FilterFile
import com.cognifide.gradle.aem.common.pkg.vault.FilterType
import com.cognifide.gradle.aem.common.pkg.vault.VaultDefinition
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.compose.BundleDependency
import com.cognifide.gradle.aem.pkg.tasks.compose.PackageDependency
import com.cognifide.gradle.aem.pkg.tasks.compose.ProjectMergingOptions
import com.cognifide.gradle.common.build.DependencyOptions
import com.cognifide.gradle.common.tasks.ZipTask
import com.cognifide.gradle.common.utils.Patterns
import java.io.File
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar

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

    @get:InputDirectory
    val metaDir = aem.obj.relativeDir(contentDir, Package.META_PATH)

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
    val vaultFilterOriginFile = aem.obj.relativeFile(metaDir, "${Package.VLT_DIR}/${FilterFile.ORIGIN_NAME}")

    @Internal
    val vaultFilterFile = aem.obj.relativeFile(vaultDir, FilterFile.BUILD_NAME)

    @Internal
    val vaultNodeTypesFile = aem.obj.relativeFile(vaultDir, Package.VLT_NODETYPES_FILE)

    @Internal
    val vaultNodeTypesSyncFile = aem.packageOptions.nodeTypesSyncFile

    @Nested
    val fileFilter = PackageFileFilter(aem)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) {
        fileFilter.apply(configurer)
    }

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it, fileProperties) }

    @get:Internal
    val fileProperties get() = mapOf("definition" to vaultDefinition.toMap())

    @Internal
    var fromConvention = true

    private var mergingOptions = ProjectMergingOptions()

    fun merging(options: ProjectMergingOptions.() -> Unit) {
        this.mergingOptions.apply(options)
    }

    private var fromProjects = mutableListOf<() -> Unit>()

    private var fromTasks = mutableListOf<() -> Unit>()

    private val bundleDependencies = mutableListOf<BundleDependency>()

    private val packageDependencies = mutableListOf<PackageDependency>()

    /**
     * Configures extra files to be observed in case of Gradle task caching.
     */
    @get:InputFiles
    val inputFiles: List<File>
        get() = mutableListOf<File>().apply {
            add(vaultNodeTypesSyncFile.get().asFile)
            addAll(bundleDependencies.flatMap { it.configuration.resolve() })
            addAll(packageDependencies.flatMap { it.configuration.resolve() })
        }.filter { it.exists() }

    override fun projectEvaluated() {
        if (fromConvention) {
            fromConvention()
        }
    }

    override fun projectsEvaluated() {
        fromProjects.forEach { it() }
        fromTasks.forEach { it() }
    }

    @TaskAction
    override fun copy() {
        vaultDefinition.apply {
            if (mergingOptions.vaultFilters && vaultFilterOriginFile.get().asFile.exists()) {
                filterElements(vaultFilterOriginFile.get().asFile)
            }

            if (vaultNodeTypesSyncFile.get().asFile.exists()) {
                nodeTypes(vaultNodeTypesSyncFile.get().asFile)
            }
        }

        super.copy()
        validator.perform(composedFile)

        common.notifier.notify("Package composed", composedFile.name)
    }

    fun fromConvention() {
        fromMeta()
        fromProject()
    }

    fun fromProject() {
        fromProject(project, mergingOptions)
    }

    fun fromProject(path: String, options: ProjectMergingOptions.() -> Unit) = fromProject(path, ProjectMergingOptions().apply(options))

    fun fromProject(path: String, options: ProjectMergingOptions = ProjectMergingOptions()) = fromProject(project.project(path), options)

    fun fromProjects(pathFilter: String, options: ProjectMergingOptions.() -> Unit) = fromProjects(pathFilter, ProjectMergingOptions().apply(options))

    fun fromProjects(pathFilter: String, options: ProjectMergingOptions = ProjectMergingOptions()) {
        project.allprojects
                .filter { Patterns.wildcard(it.path, pathFilter) }
                .forEach { fromProject(it, options) }
    }

    fun fromSubprojects(options: ProjectMergingOptions.() -> Unit) = fromSubprojects(ProjectMergingOptions().apply(options))

    fun fromSubprojects(options: ProjectMergingOptions = ProjectMergingOptions()) {
        if (project == project.rootProject) {
            fromProjects(":*", options)
        } else {
            fromProjects("${project.path}:*", options)
        }
    }

    fun fromMeta() = fromMeta(metaDir)

    fun fromMeta(metaDir: Any) {
        into(Package.META_PATH) { spec ->
            spec.from(metaDir)
            fileFilterDelegate(spec)
        }
    }

    fun fromProject(project: Project, options: ProjectMergingOptions.() -> Unit) = fromProject(project, ProjectMergingOptions().apply(options))

    fun fromProject(project: Project, options: ProjectMergingOptions = ProjectMergingOptions()) {
        fromProjects.add {
            val other by lazy { AemExtension.of(project) }

            if (project.plugins.hasPlugin(PackagePlugin.ID)) {
                options.composeTasks(other).forEach {
                    fromPackage(it, options)
                }
            }

            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                options.bundleTasks(other).forEach {
                    fromBundle(it, options)
                }
            }

            options.extraTasks(other).forEach {
                dependsOn(it)
            }
        }
    }

    fun fromPackage(composeTaskPath: String) {
        fromPackage(common.tasks.get(composeTaskPath, PackageCompose::class.java), ProjectMergingOptions())
    }

    @Suppress("ComplexMethod")
    private fun fromPackage(other: PackageCompose, options: ProjectMergingOptions) {
        fromTasks.add {
            if (this@PackageCompose != other) {
                dependsOn(other.dependsOn)
            }

            if (options.composeContent) {
                val contentDir = other.contentDir.file(Package.JCR_ROOT).get().asFile
                if (contentDir.exists()) {
                    into(Package.JCR_ROOT) { spec ->
                        spec.from(contentDir)
                        fileFilterDelegate(spec)
                    }
                }
            }

            if (options.vaultFilters && other.vaultFilterFile.get().asFile.exists()) {
                vaultDefinition.filterElements(other.vaultFilterFile.get().asFile)
            }

            if (options.vaultNodeTypes && other.vaultNodeTypesFile.get().asFile.exists()) {
                vaultDefinition.nodeTypes(other.vaultNodeTypesFile.get().asFile)
            }

            if (options.vaultProperties) {
                other.vaultDefinition.properties.get().forEach { (name, value) ->
                    vaultDefinition.properties.put(name, value) // TODO putIfAbsent (make it more lazy)
                }
            }

            if (options.vaultHooks) {
                val hooksDir = other.contentDir.dir(Package.VLT_HOOKS_PATH).get().asFile
                if (hooksDir.exists()) {
                    into(Package.VLT_HOOKS_PATH) { spec ->
                        spec.from(hooksDir)
                        fileFilterDelegate(spec)
                    }
                }
            }

            if (options.bundleDependent) {
                other.bundleDependencies.forEach {
                    fromJarInternal(it.file, it.installPath, it.vaultFilter)
                }
            }

            if (options.packageDependent) {
                other.packageDependencies.forEach {
                    fromZipInternal(it.file, it.storagePath, it.vaultFilter)
                }
            }
        }
    }

    fun fromBundle(composeTaskPath: String) {
        fromBundle(common.tasks.get(composeTaskPath, BundleCompose::class.java), ProjectMergingOptions())
    }

    private fun fromBundle(bundle: BundleCompose, options: ProjectMergingOptions) {
        if (options.bundleBuilt) {
            fromJar(bundle, Package.bundlePath(bundle.installPath.get(), bundle.installRunMode.orNull), bundle.vaultFilter.get())
        }
    }

    fun fromJar(
        dependencyOptions: DependencyOptions.() -> Unit,
        installPath: String? = null,
        vaultFilter: Boolean? = null
    ) {
        val dependency = DependencyOptions.create(project, dependencyOptions)
        bundleDependencies.add(BundleDependency(aem, dependency,
                installPath ?: this.bundlePath.get(),
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromJar(dependencyNotation: Any, installPath: String? = null, vaultFilter: Boolean? = null) {
        val dependency = DependencyOptions.create(project, dependencyNotation)
        bundleDependencies.add(BundleDependency(aem, dependency,
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

    fun fromZip(
        dependencyOptions: DependencyOptions.() -> Unit,
        storagePath: String? = null,
        vaultFilter: Boolean? = null
    ) {
        val dependency = DependencyOptions.create(project) { apply(dependencyOptions); ext = "zip" }
        packageDependencies.add(PackageDependency(aem, dependency,
                storagePath ?: packagePath.get(),
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromZip(dependencyNotation: String, storagePath: String? = null, vaultFilter: Boolean? = null) {
        fromZip(StringUtils.appendIfMissing(dependencyNotation, "@zip") as Any, storagePath, vaultFilter)
    }

    fun fromZip(dependencyNotation: Any, storagePath: String? = null, vaultFilter: Boolean? = null) {
        val dependency = DependencyOptions.create(project, dependencyNotation)
        packageDependencies.add(PackageDependency(aem, dependency,
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

    fun fromSubpackage(composeTaskPath: String, storagePath: String? = null, vaultFilter: Boolean? = null) {
        fromTasks.add {
            val other = common.tasks.pathed(composeTaskPath).get() as PackageCompose

            dependsOn(other)

            val file = other.composedFile
            val effectivePath = "${storagePath ?: this.packagePath}/${other.vaultDefinition.group}"

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
        val effectivePackagePath = "${packagePath ?: this.packagePath}/$effectivePackageDir"

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
