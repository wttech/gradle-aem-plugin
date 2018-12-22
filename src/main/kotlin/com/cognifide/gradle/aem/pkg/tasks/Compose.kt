package com.cognifide.gradle.aem.pkg.tasks

import com.cognifide.gradle.aem.bundle.BundleJar
import com.cognifide.gradle.aem.bundle.BundlePlugin
import com.cognifide.gradle.aem.common.*
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.pkg.Package
import com.cognifide.gradle.aem.pkg.PackageFileFilter
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.tooling.vlt.VltFilter
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.util.regex.Pattern
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.*
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
    val bundleFiles = project.configurations.create(BUNDLE_FILES_CONFIGURATION) { it.isTransitive = false }

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
            val paths = listOf(aem.config.packageMetaCommonRoot, "$contentPath/${Package.META_PATH}")

            return paths.asSequence()
                    .filter { !it.isBlank() }
                    .map { File(it) }
                    .filter { it.exists() }
                    .toList()
        }

    /**
     * Additional entries added to file 'META-INF/vault/properties.xml'.
     */
    @Input
    var vaultProperties: Map<String, Any> = VAULT_PROPERTIES_DEFAULT

    fun vaultProperty(name: String, value: String) { vaultProperties += mapOf(name to value) }

    @get:Internal
    @get:JsonIgnore
    val vaultPath: String
        get() = "$contentPath/${Package.VLT_PATH}"

    @get:Internal
    @get:JsonIgnore
    val vaultFilterPath: String
        get() = "$vaultPath/${VltFilter.BUILD_NAME}"

    @get:Internal
    @get:JsonIgnore
    val vaultNodeTypesPath: String
        get() = "$vaultPath/${Package.VLT_NODETYPES_FILE}"

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

    @get:Internal
    val vaultFilterRoots: List<String>
        get() = vaultFilters.map { it.attr("root") }

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
            FileOperations.copyResources(Package.META_PATH, metaDir, true)
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
            val configuredOptions = ProjectOptions().apply(options)

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
        fromCompose(project.tasks.getByPath(composeTaskPath) as Compose)
    }

    fun fromCompose(other: Compose) = fromCompose(other, ProjectOptions())

    private fun fromCompose(other: Compose, options: ProjectOptions) {
        fromTasks.add {
            if (this@Compose != other) {
                dependsOn(other.dependsOn)
            }

            if (options.bundleDependent) {
                fromJarsInternal(other.bundleFiles.resolve(), options.bundlePath(other.bundlePath, other.bundleRunMode))
            }

            if (options.vaultFilters) {
                extractVaultFilters(other)
            }

            if (options.vaultNodeTypes) {
                extractVaultNodeTypes(other)
            }

            if (options.composeContent) {
                val contentDir = File("${other.contentPath}/${Package.JCR_ROOT}")
                if (contentDir.exists()) {
                    into(Package.JCR_ROOT) { spec ->
                        spec.from(contentDir)
                        fileFilterDelegate(spec)
                    }
                }
            }

            if (options.vaultHooks) {
                val hooksDir = File("${other.contentPath}/${Package.VLT_HOOKS_PATH}")
                if (hooksDir.exists()) {
                    into(Package.VLT_HOOKS_PATH) { spec ->
                        spec.from(hooksDir)
                        fileFilterDelegate(spec)
                    }
                }
            }
        }
    }

    fun fromBundle(bundle: BundleJar) = fromBundle(bundle, ProjectOptions())

    private fun fromBundle(bundle: BundleJar, options: ProjectOptions) {
        if (options.bundleBuilt) {
            fromJar(bundle.jar, options.bundlePath(bundle.installPath, bundle.installRunMode))
        }
    }

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
            into("${Package.JCR_ROOT}/${bundlePath ?: this.bundlePath}") { spec ->
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

    class ProjectOptions {

        /**
         * Determines if JCR content from separate project should be included in composed package.
         */
        var composeContent: Boolean = true

        var composeTasks: AemExtension.() -> Collection<Compose> = { project.tasks.withType(Compose::class.java).toList() }

        var vaultHooks: Boolean = true

        var vaultFilters: Boolean = true

        var vaultNodeTypes: Boolean = true

        /**
         * Determines if OSGi bundle built in separate project should be included in composed package.
         */
        var bundleBuilt: Boolean = true

        var bundleTasks: AemExtension.() -> Collection<Jar> = { project.tasks.withType(Jar::class.java).toList() }

        var bundleDependent: Boolean = true

        var bundlePath: String? = null

        var bundleRunMode: String? = null

        internal fun bundlePath(otherBundlePath: String, otherBundleRunMode: String?): String {
            val effectiveBundlePath = bundlePath ?: otherBundlePath
            val effectiveBundleRunMode = bundleRunMode ?: otherBundleRunMode

            var result = effectiveBundlePath
            if (!effectiveBundleRunMode.isNullOrBlank()) {
                result = "$effectiveBundlePath.$effectiveBundleRunMode"
            }

            return result
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