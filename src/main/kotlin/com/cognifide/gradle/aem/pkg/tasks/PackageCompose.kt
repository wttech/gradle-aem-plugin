package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.build.DependencyOptions
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageFile
import com.cognifide.gradle.aem.common.pkg.PackageFileFilter
import com.cognifide.gradle.aem.common.pkg.vlt.VltDefinition
import com.cognifide.gradle.aem.common.pkg.vlt.VltFilter
import com.cognifide.gradle.aem.common.pkg.vlt.VltFilterType
import com.cognifide.gradle.aem.common.tasks.ZipTask
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.compose.BundleDependency
import com.cognifide.gradle.aem.pkg.tasks.compose.PackageDependency
import com.cognifide.gradle.aem.pkg.tasks.compose.ProjectMergingOptions
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.util.regex.Pattern
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar

@Suppress("TooManyFunctions", "LargeClass")
open class PackageCompose : ZipTask() {

    /**
     * Absolute path to JCR content to be included in CRX package.
     *
     * Must be absolute or relative to current working directory.
     */
    @Internal
    var contentDir: File = aem.packageOptions.contentDir

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     */
    @Input
    var bundlePath: String = aem.packageOptions.installPath

    /**
     * Content path for CRX sub-packages being placed in CRX package being built.
     */
    @Internal
    var packagePath: String = aem.packageOptions.storagePath

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var metaDefaults: Boolean = true

    @Internal
    val metaDir = AemTask.temporaryDir(project, name, Package.META_PATH)

    /**
     * CRX package Vault files will be composed from given sources.
     * Missing files required by package within installation will be auto-generated if 'vaultCopyMissingFiles' is enabled.
     */
    @get:Internal
    @get:JsonIgnore
    val metaDirs: List<File>
        get() {
            val dirs = listOf(
                    aem.packageOptions.metaCommonDir,
                    File(contentDir, Package.META_PATH)
            )

            return dirs.asSequence()
                    .filter { it.exists() }
                    .toList()
        }

    /**
     * Defines properties being used to generate CRX package metadata files.
     */
    @Nested
    val vaultDefinition = VltDefinition(aem)

    fun vaultDefinition(options: VltDefinition.() -> Unit) {
        vaultDefinition.apply(options)
    }

    @get:Internal
    @get:JsonIgnore
    val vaultDir: File
        get() = File(contentDir, Package.VLT_PATH)

    @get:Internal
    @get:JsonIgnore
    val vaultFilterFile: File
        get() = File(vaultDir, VltFilter.BUILD_NAME)

    @get:Internal
    @get:JsonIgnore
    val vaultNodeTypesFile: File
        get() = File(vaultDir, Package.VLT_NODETYPES_FILE)

    @Nested
    val fileFilter = PackageFileFilter(aem)

    fun fileFilter(configurer: PackageFileFilter.() -> Unit) {
        fileFilter.apply(configurer)
    }

    @Internal
    var fileFilterDelegate: ((CopySpec) -> Unit) = { fileFilter.filter(it, fileProperties) }

    @get:Internal
    val fileProperties
        get() = mapOf("definition" to vaultDefinition)

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

    init {
        description = "Composes CRX package from JCR content and built OSGi bundles"

        archiveBaseName.set(aem.baseName)
        duplicatesStrategy = DuplicatesStrategy.WARN

        doLast { aem.notifier.notify("Package composed", archiveFileName.get()) }
    }

    @Suppress("ComplexMethod")
    override fun projectEvaluated() {
        vaultDefinition.ensureDefaults()

        if (bundlePath.isBlank()) {
            throw AemException("Bundle path cannot be blank")
        }

        if (fromConvention) {
            fromConvention()
        }
    }

    override fun projectsEvaluated() {
        bundleDependencies.forEach { inputs.files(it.configuration) }
        packageDependencies.forEach { inputs.files(it.configuration) }
        metaDirs.forEach { dir -> inputs.dir(dir) }
        fromProjects.forEach { it() }
        fromTasks.forEach { it() }
    }

    @TaskAction
    override fun copy() {
        copyMetaFiles()
        super.copy()
    }

    private fun copyMetaFiles() {
        if (metaDir.exists()) {
            metaDir.deleteRecursively()
        }

        metaDir.mkdirs()

        if (metaDirs.isEmpty()) {
            logger.info("None of package metadata directories exist: $metaDirs. Only generated defaults will be used.")
        } else {
            metaDirs.onEach { dir ->
                logger.info("Copying package metadata files from path: '$dir'")

                FileUtils.copyDirectory(dir, metaDir)
            }
        }

        val filterBackup = File(metaDir, "${Package.VLT_DIR}/${VltFilter.ROOTS_NAME}")
        val filterTemplate = File(metaDir, "${Package.VLT_DIR}/${VltFilter.BUILD_NAME}")

        if (mergingOptions.vaultFilters && filterTemplate.exists() && !filterBackup.exists()) {
            filterTemplate.renameTo(filterBackup)
        }

        if (metaDefaults) {
            logger.info("Providing package metadata files in directory: '$metaDir")
            FileOperations.copyResources(Package.META_RESOURCES_PATH, metaDir, true)
        }

        if (mergingOptions.vaultFilters && filterBackup.exists()) {
            logger.info("Considering original package Vault filters specified in file: '$filterBackup'")
            extractVaultFilters(filterBackup)
        }
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

    fun fromMeta(metaDir: File) {
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
                    fromCompose(it, options)
                }
            }

            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                options.bundleTasks(other).forEach {
                    fromBundle(other.tasks.bundle(it), options)
                }
            }
        }
    }

    fun fromCompose(composeTaskPath: String) {
        fromCompose(aem.tasks.get(composeTaskPath, PackageCompose::class.java), ProjectMergingOptions())
    }

    @Suppress("ComplexMethod")
    private fun fromCompose(other: PackageCompose, options: ProjectMergingOptions) {
        fromTasks.add {
            if (this@PackageCompose != other) {
                dependsOn(other.dependsOn)
            }

            if (options.composeContent) {
                val contentDir = File(other.contentDir, Package.JCR_ROOT)
                if (contentDir.exists()) {
                    into(Package.JCR_ROOT) { spec ->
                        spec.from(contentDir)
                        fileFilterDelegate(spec)
                    }
                }
            }

            if (options.vaultFilters) {
                extractVaultFilters(other.vaultFilterFile)
            }

            if (options.vaultNodeTypes) {
                extractVaultNodeTypes(other.vaultNodeTypesFile)
            }

            if (options.vaultHooks) {
                val hooksDir = File(other.contentDir, Package.VLT_HOOKS_PATH)
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

    fun fromBundle(jarTaskPath: String) {
        fromBundle(aem.tasks.bundle(jarTaskPath), ProjectMergingOptions())
    }

    private fun fromBundle(bundle: BundleJar, options: ProjectMergingOptions) {
        if (options.bundleBuilt) {
            fromJar(bundle.jar, Package.bundlePath(bundle.installPath, bundle.installRunMode), bundle.vaultFilter)
        }
    }

    fun fromJar(dependencyOptions: DependencyOptions.() -> Unit, installPath: String? = null, vaultFilter: Boolean? = null) {
        val dependency = DependencyOptions.create(aem, dependencyOptions)
        bundleDependencies.add(BundleDependency(aem, dependency,
                installPath ?: this.bundlePath,
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromJar(dependencyNotation: String, installPath: String? = null, vaultFilter: Boolean? = null) {
        val dependency = DependencyOptions.create(aem, dependencyNotation)
        bundleDependencies.add(BundleDependency(aem, dependency,
                installPath ?: this.bundlePath,
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromJar(bundle: Jar, bundlePath: String? = null, vaultFilter: Boolean? = null) {
        fromTasks.add {
            dependsOn(bundle)
            fromJarInternal(bundle.archiveFile.get().asFile, bundlePath, vaultFilter)
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
        val effectiveBundlePath = bundlePath ?: this.bundlePath

        if (vaultFilter ?: mergingOptions.vaultFilters) {
            vaultDefinition.filter("$effectiveBundlePath/${jar.name}")
        }

        into("${Package.JCR_ROOT}/$effectiveBundlePath") { spec ->
            spec.from(jar)
            fileFilterDelegate(spec)
        }
    }

    fun fromZip(dependencyOptions: DependencyOptions.() -> Unit, storagePath: String? = null, vaultFilter: Boolean? = null) {
        val dependency = DependencyOptions.create(aem) { apply(dependencyOptions); ext = "zip" }
        packageDependencies.add(PackageDependency(aem, dependency,
                storagePath ?: packagePath,
                vaultFilter ?: mergingOptions.vaultFilters
        ))
    }

    fun fromZip(dependencyNotation: String, storagePath: String? = null, vaultFilter: Boolean? = null) {
        val dependency = DependencyOptions.create(aem, StringUtils.appendIfMissing(dependencyNotation, "@zip"))
        packageDependencies.add(PackageDependency(aem, dependency,
                storagePath ?: packagePath,
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

    private fun fromZipInternal(file: File, packagePath: String? = null, vaultFilter: Boolean? = null) {
        val effectivePackageDir = aem.packageOptions.storageDir(PackageFile(file))
        val effectivePackagePath = "${packagePath ?: this.packagePath}/$effectivePackageDir"

        if (vaultFilter ?: mergingOptions.vaultFilters) {
            vaultDefinition.filter("$effectivePackagePath/${file.name}", type = VltFilterType.FILE)
        }

        into("${Package.JCR_ROOT}/$effectivePackagePath") { spec ->
            spec.from(file)
            fileFilterDelegate(spec)
        }
    }

    private fun extractVaultFilters(file: File) {
        if (file.exists()) {
            vaultDefinition.filterElements.addAll(VltFilter(file).elements)
        }
    }

    private fun extractVaultNodeTypes(file: File) {
        if (!file.exists()) {
            return
        }

        file.forEachLine { line ->
            if (NODE_TYPES_LIB.matcher(line.trim()).matches()) {
                vaultDefinition.nodeTypeLibs.add(line)
            } else {
                vaultDefinition.nodeTypeLines.add(line)
            }
        }
    }

    companion object {
        const val NAME = "packageCompose"

        val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")
    }
}