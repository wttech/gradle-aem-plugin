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
import com.cognifide.gradle.aem.common.tasks.ZipTask
import com.cognifide.gradle.aem.common.utils.Patterns
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.tasks.compose.ProjectOptions
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
import org.jsoup.nodes.Element

@Suppress("TooManyFunctions")
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
     * Suffix added to bundle path effectively allowing to install bundles only on specific instances.
     *
     * @see <https://helpx.adobe.com/experience-manager/6-4/sites/deploying/using/configure-runmodes.html#Definingadditionalbundlestobeinstalledforarunmode>
     */
    @Input
    @Optional
    var bundleRunMode: String? = null

    /**
     * Dependent OSGi bundles to be resolved from repositories and put into CRX package being built.
     */
    @Internal
    val bundleFiles = project.configurations.create(BUNDLES_CONFIGURATION) { it.isTransitive = false }

    /**
     * Dependent CRX packages to be resolved from repositories and put into CRX package being built.
     */
    @Internal
    val packageFiles = project.configurations.create(PACKAGES_CONFIGURATION) { it.isTransitive = false }

    /**
     * Content path for CRX sub-packages being placed in CRX package being built.
     */
    @Input
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
    var vaultFilterDefault: (PackageCompose) -> Element = { VltFilter.createElement(it.bundlePath) }

    @Internal
    var fromConvention = true

    private var fromProjects = mutableListOf<() -> Unit>()

    private var fromTasks = mutableListOf<() -> Unit>()

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
        inputs.files(bundleFiles)
        inputs.files(packageFiles)
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
            logger.info("None of metadata directories exist: $metaDirs. Only generated defaults will be used.")
        } else {
            metaDirs.onEach { dir ->
                logger.info("Copying metadata files from path: '${dir.absolutePath}'")

                FileUtils.copyDirectory(dir, metaDir)
            }
        }

        if (metaDefaults) {
            FileOperations.copyResources(Package.META_RESOURCES_PATH, metaDir, true)
        }
    }

    fun fromConvention() {
        fromMeta()
        fromProject()
    }

    fun fromProject(path: String, options: ProjectOptions.() -> Unit = {}) = fromProject(project.project(path), options)

    fun fromProject(options: ProjectOptions.() -> Unit = {}) = fromProject(project, options)

    fun fromProjects(pathFilter: String, options: ProjectOptions.() -> Unit = {}) {
        project.allprojects
                .filter { Patterns.wildcard(it.path, pathFilter) }
                .forEach { fromProject(it, options) }
    }

    fun fromSubprojects(options: ProjectOptions.() -> Unit = {}) {
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

    fun fromProject(project: Project, options: ProjectOptions.() -> Unit = {}) {
        fromProjects.add {
            val other by lazy { AemExtension.of(project) }
            val configuredOptions by lazy { ProjectOptions().apply(options) }

            if (project.plugins.hasPlugin(PackagePlugin.ID)) {
                configuredOptions.composeTasks(other).forEach {
                    fromCompose(it, configuredOptions)
                }
            }

            if (project.plugins.hasPlugin(BundlePlugin.ID)) {
                configuredOptions.bundleTasks(other).forEach {
                    fromBundle(other.tasks.bundle(it), configuredOptions)
                }
            }
        }
    }

    fun fromCompose(composeTaskPath: String) {
        fromCompose(aem.tasks.get(composeTaskPath, PackageCompose::class.java), ProjectOptions())
    }

    @Suppress("ComplexMethod")
    private fun fromCompose(other: PackageCompose, options: ProjectOptions) {
        fromTasks.add {
            if (this@PackageCompose != other) {
                dependsOn(other.dependsOn)
            }

            if (options.bundleDependent) {
                other.bundleFiles.resolve().forEach {
                    fromJarInternal(it, options.bundlePath(other.bundlePath, other.bundleRunMode))
                }
            }

            if (options.packageDependent) {
                other.packageFiles.resolve().forEach {
                    fromZipInternal(it, options.packagePath(other.packagePath))
                }
            }

            if (options.vaultFilters) {
                extractVaultFilters(other)
            }

            if (options.vaultNodeTypes) {
                extractVaultNodeTypes(other)
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

            if (options.vaultHooks) {
                val hooksDir = File(other.contentDir, Package.VLT_HOOKS_PATH)
                if (hooksDir.exists()) {
                    into(Package.VLT_HOOKS_PATH) { spec ->
                        spec.from(hooksDir)
                        fileFilterDelegate(spec)
                    }
                }
            }
        }
    }

    fun fromBundle(jarTaskPath: String) {
        fromBundle(aem.tasks.bundle(jarTaskPath), ProjectOptions())
    }

    private fun fromBundle(bundle: BundleJar, options: ProjectOptions) {
        if (options.bundleBuilt) {
            fromJar(bundle.jar, options.bundlePath(bundle.installPath, bundle.installRunMode))
        }
    }

    fun fromJar(dependencyOptions: DependencyOptions.() -> Unit) {
        DependencyOptions.add(aem, BUNDLES_CONFIGURATION, dependencyOptions)
    }

    fun fromJar(dependencyNotation: String) {
        DependencyOptions.add(aem, BUNDLES_CONFIGURATION, dependencyNotation)
    }

    fun fromJar(bundle: Jar, bundlePath: String? = null) {
        fromTasks.add {
            dependsOn(bundle)
            fromJarInternal(bundle.archiveFile.get().asFile, bundlePath)
        }
    }

    fun fromJar(bundle: File, bundlePath: String? = null) = fromJars(listOf(bundle), bundlePath)

    fun fromJars(bundles: Collection<File>, bundlePath: String? = null) {
        fromTasks.add {
            bundles.forEach { fromJarInternal(it, bundlePath) }
        }
    }

    private fun fromJarInternal(bundle: File, bundlePath: String? = null) {
        into("${Package.JCR_ROOT}/${bundlePath ?: this.bundlePath}") { spec ->
            spec.from(bundle)
            fileFilterDelegate(spec)
        }
    }

    fun fromZip(dependencyOptions: DependencyOptions.() -> Unit) {
        DependencyOptions.add(aem, PACKAGES_CONFIGURATION) { apply(dependencyOptions); ext = "zip" }
    }

    fun fromZip(dependencyNotation: String) {
        DependencyOptions.add(aem, PACKAGES_CONFIGURATION, StringUtils.appendIfMissing(dependencyNotation, "@zip"))
    }

    fun fromZip(pkg: File, storagePath: String? = null) = fromZips(listOf(pkg), storagePath)

    fun fromZips(packages: Collection<File>, storagePath: String? = null) {
        fromTasks.add {
            packages.forEach { fromZipInternal(it, storagePath) }
        }
    }

    private fun fromZipInternal(pkg: File, storagePath: String? = null) {
        val pkgPath = PackageFile(pkg).run { "${storagePath ?: aem.packageOptions.storagePath}/$group/${pkg.name}" }

        into("${Package.JCR_ROOT}/$pkgPath") { spec ->
            spec.from(pkg)
            fileFilterDelegate(spec)
        }
    }

    private fun extractVaultFilters(other: PackageCompose) {
        if (other.vaultFilterFile.exists()) {
            vaultDefinition.filterElements.addAll(VltFilter(other.vaultFilterFile).rootElements)
        } else if (project.plugins.hasPlugin(BundlePlugin.ID)) {
            vaultDefinition.filterElements.add(vaultFilterDefault(other))
        }
    }

    private fun extractVaultNodeTypes(other: PackageCompose) {
        val file = other.vaultNodeTypesFile
        if (file.exists()) {
            file.forEachLine { line ->
                if (NODE_TYPES_LIB.matcher(line.trim()).matches()) {
                    vaultDefinition.nodeTypeLibs.add(line)
                } else {
                    vaultDefinition.nodeTypeLines.add(line)
                }
            }
        }
    }

    companion object {
        const val NAME = "packageCompose"

        const val BUNDLES_CONFIGURATION = "bundles"

        const val PACKAGES_CONFIGURATION = "packages"

        val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")
    }
}