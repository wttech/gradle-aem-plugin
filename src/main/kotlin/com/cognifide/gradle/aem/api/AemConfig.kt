package com.cognifide.gradle.aem.api

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.LineSeparator
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.cognifide.gradle.aem.pkg.deploy.DeployException
import com.fasterxml.jackson.annotation.JsonIgnore
import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.util.ConfigureUtil
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Aggregated collection of AEM related configuration.
 *
 * Content paths which are used to compose a CRX package are being processed by copy task,
 * which automatically mark them as inputs so package is being rebuild on any JCR content or Vault files change.
 *
 * TODO https://docs.gradle.org/4.6/userguide/custom_tasks.html#sec:declaring_and_using_command_line_options
 */
class AemConfig(
        @Transient
        @get:JsonIgnore
        private val project: Project
) : Serializable {

    /**
     * Allows to read project property specified in command line and system property as a fallback.
     */
    @Internal
    @get:JsonIgnore
    val propParser = PropertyParser(project)

    /**
     * Determines current environment to be used in e.g package deployment.
     */
    @Input
    val environment: String = propParser.string("aem.env", {
        System.getenv("AEM_ENV") ?: "local"
    })

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Input
    var instances: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Determines instances involved in CRX package deployment (filters preconfigured instances).
     */
    @Input
    var instanceName: String = propParser.string("aem.instance.name", "$environment-*")

    /**
     * Forces instances involved in e.g CRX package deployment (uses explicit instances configuration).
     */
    @Input
    var instanceList: String = propParser.string("aem.instance.list", "")

    /**
     * Determines instance which will be used when CRX package activation from author to publishers
     * will be performed (only if distributed deploy is enabled).
     */
    @Input
    var instanceAuthorName: String = propParser.string("aem.instance.author.name", "$environment-${InstanceType.AUTHOR}*")

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     */
    @Input
    var instanceConnectionTimeout: Int = propParser.int("aem.instance.connectionTimeout", 5000)

    /**
     * Determines if connection to untrusted (e.g. self-signed) SSL certificates should be allowed.
     * By default allows all SSL connections.
     */
    @Input
    var instanceConnectionUntrustedSsl: Boolean = propParser.boolean("aem.instance.connectionUntrustedSsl", true)

    /**
     * Perform deploy action (upload, install or activate) in parallel to multiple instances at once.
     */
    @Input
    var deployParallel: Boolean = propParser.boolean("aem.deploy.parallel", true)

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified.
     */
    @Input
    var deploySnapshots: List<String> = propParser.list("aem.deploy.snapshots")

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    var deployDistributed: Boolean = propParser.boolean("aem.deploy.distributed", false)

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean = propParser.boolean("aem.upload.force", true)

    /**
     * Repeat upload when failed (brute-forcing).
     */
    @Input
    var uploadRetryTimes: Int = propParser.int("aem.upload.retry.times", 3)

    /**
     * Time to wait after repeating failed upload.
     */
    @Input
    var uploadRetryDelay: Long = propParser.long("aem.upload.retry.delay", TimeUnit.SECONDS.toMillis(30))

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var installRecursive: Boolean = propParser.boolean("aem.install.recursive", true)

    /**
     * Repeat install when failed (brute-forcing).
     */
    @Input
    var installRetryTimes: Int = propParser.int("aem.install.retry.times", 1)

    /**
     * Time to wait after repeating failed install.
     */
    @Input
    var installRetryDelay: Long = propParser.long("aem.install.retry.delay", TimeUnit.SECONDS.toMillis(30))

    /**
     * Defines behavior for access control handling included in rep:policy nodes being a part of CRX package content.
     *
     * @see <https://jackrabbit.apache.org/filevault/apidocs/org/apache/jackrabbit/vault/fs/io/AccessControlHandling.html>
     */
    @Input
    var acHandling: String = "merge_preserve"

    /**
     * Absolute path to JCR content to be included in CRX package.
     * Must be absolute or relative to current working directory.
     */
    @Input
    var contentPath: String = "${project.file("src/main/content")}"

    /**
     * Content path for bundle jars being placed in CRX package.
     */
    @Input
    var bundlePath: String = if (project == project.rootProject) {
        "/apps/${project.name}/install"
    } else {
        "/apps/${project.rootProject.name}/${project.name}/install"
    }

    /**
     * Determines package in which OSGi bundle being built contains its classes.
     * Basing on that value, there will be:
     *
     * - generated OSGi specific manifest instructions like 'Bundle-SymbolicName', 'Export-Package'.
     * - generated AEM specific manifest instructions like 'Sling-Model-Packages'.
     * - performed additional component stability checks during 'aemAwait'
     */
    @Input
    var bundlePackage: String = ""

    /**
     * Determines how conflicts will be resolved when coincidental classes will be detected.
     * Useful to combine Java sources with Kotlin, Scala etc.
     *
     * @see <http://bnd.bndtools.org/heads/private_package.html>
     */
    @Input
    var bundlePackageOptions: String = "-split-package:=merge-first"

    /**
     * Enable or disable support for auto-generating OSGi specific JAR manifest attributes
     * like 'Bundle-SymbolicName', 'Export-Package' or AEM specific like 'Sling-Model-Packages'
     * using 'bundlePackage' property.
     */
    @Input
    var bundleManifestAttributes: Boolean = true

    /**
     * Automatically determine local package to be uploaded.
     */
    @get:Internal
    @get:JsonIgnore
    val packageFile: File
        get() {
            if (!localPackagePath.isBlank()) {
                val configFile = File(localPackagePath)
                if (configFile.exists()) {
                    return configFile
                }
            }

            val archiveFile = pkg(project).archivePath
            if (archiveFile.exists()) {
                return archiveFile
            }

            throw DeployException("Local package not found under path: '${archiveFile.absolutePath}'. Is it built already?")
        }

    @get:Internal
    @get:JsonIgnore
    val packageFileName: String
        get() = pkg(project).archiveName

    /**
     * Determines built CRX package name (visible in package manager).
     */
    @Input
    var packageName: String = if (isUniqueProjectName()) {
        project.name
    } else {
        "${namePrefix()}-${project.name}"
    }

    /**
     * Custom path to composed CRX package being uploaded.
     *
     * Default: [automatically determined]
     */
    @Input
    var localPackagePath: String = ""

    /**
     * Custom path to CRX package that is uploaded on AEM instance.
     *
     * Default: [automatically determined]
     */
    @Input
    var remotePackagePath: String = ""

    /**
     * Exclude files being a part of CRX package.
     */
    @Input
    var filesExcluded: MutableList<String> = mutableListOf(
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
    var filesExpanded: MutableList<String> = mutableListOf("**/${PackagePlugin.VLT_PATH}/*.xml")

    /**
     * Build date used as base for calculating 'created' and 'buildCount' package properties.
     */
    @Internal
    var buildDate: Date = propParser.date("aem.buildDate", Date())

    /**
     * Define here custom properties that can be used in CRX package files like 'META-INF/vault/properties.xml'.
     * Could override predefined properties provided by plugin itself.
     */
    @Input
    var fileProperties: MutableMap<String, Any> = mutableMapOf()

    /**
     * Ensures that for directory 'META-INF/vault' default files will be generated when missing:
     * 'config.xml', 'filter.xml', 'properties.xml' and 'settings.xml'.
     */
    @Input
    var vaultCopyMissingFiles: Boolean = true

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     * Must be absolute or relative to current working directory.
     */
    @Input
    var vaultFilesPath: String = project.rootProject.file("src/main/resources/${PackagePlugin.VLT_PATH}").toString()

    /**
     * Global options which are being applied to any Vault related command like 'aemVault' or 'aemCheckout'.
     */
    @Input
    var vaultGlobalOptions: String = propParser.string("aem.vlt.globalOptions", "--credentials {{instance.credentials}}")

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    @Input
    var vaultLineSeparator: String = propParser.string("aem.vlt.lineSeparator", "LF")

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
     * Configure default task dependency assignments while including dependant project content.
     * Simplifies multi-module project configuration.
     */
    @Input
    var dependContentTaskNames: List<String> = mutableListOf(
            ComposeTask.NAME + ComposeTask.DEPENDENCIES_SUFFIX
    )

    /**
     * Path in which local AEM instances will be stored.
     *
     * Default: "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
     */
    @Input
    var createPath: String = "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    @Input
    var createFilesPath: String = project.rootProject.file("src/main/resources/${InstancePlugin.FILES_PATH}").toString()

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    @Input
    var createFilesExpanded: MutableList<String> = mutableListOf("**/*.properties", "**/*.sh", "**/*.bat", "**/*.xml", "**/start", "**/stop")

    /**
     * Hook called only when instance is up first time.
     */
    @Internal
    @get:JsonIgnore
    var upInitializer: (LocalHandle, InstanceSync) -> Unit = { _, _ -> }

    /**
     * Time in milliseconds used as interval between next instance stability checks being performed.
     * Optimization could be necessary only when instance is heavily loaded.
     */
    @Input
    var awaitStableInterval: Long = propParser.long("aem.await.interval", TimeUnit.SECONDS.toMillis(1))

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    @Input
    var awaitStableTimes: Long = propParser.long("aem.await.times", 60 * 5)

    /**
     * If there is still some unstable instance left, then fail build except just logging warning.
     */
    @Input
    var awaitFail: Boolean = propParser.boolean("aem.await.fail", true)

    /**
     * Hook for customizing instance state provider used within stable checking.
     */
    @Internal
    @get:JsonIgnore
    var awaitStableState: (InstanceState) -> Int = { it.bundleState.hashCode() }

    /**
     * After each await interval, instance stability check is being performed.
     * This value is a HTTP connection timeout (in millis) which must be smaller than interval to avoid race condition.
     */
    @Input
    var awaitStableTimeout: Int = propParser.int("aem.await.stable.timeout", (0.9 * awaitStableInterval.toDouble()).toInt())

    /**
     * Hook for customizing instance stability check.
     */
    @Internal
    @get:JsonIgnore
    var awaitStableCheck: (InstanceState) -> Boolean = { it.bundleState.stable }

    /**
     * Number of intervals / additional instance stability checks to assure all stable instances.
     * This mechanism protect against temporary stable states.
     */
    @Input
    var awaitStableAssurances: Long = propParser.long("aem.await.stable.assurances", 5L)

    /**
     * Hook for customizing instance health check.h
     */
    @Internal
    @get:JsonIgnore
    var awaitHealthcheck: (InstanceState) -> Boolean = {
        if (bundlePackage.isNotBlank()) {
            it.componentState.stable("$bundlePackage.*")
        } else {
            it.componentState.stable
        }
    }

    /**
     * After each await interval, instance stability check is being performed.
     * This value is a HTTP connection timeout (in millis) which must be smaller than interval to avoid race condition.
     */
    @Input
    var awaitHealthTimeout: Int = propParser.int("aem.await.health.timeout", 10000)


    /**
     * Time in milliseconds to postpone instance stability checks after triggering instances restart.
     */
    @Input
    var reloadDelay: Long = propParser.long("aem.reload.delay", TimeUnit.SECONDS.toMillis(10))

    /**
     * Satisfy is a lazy task, which means that it will not install package that is already installed.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Input
    var satisfyRefreshing: Boolean = propParser.boolean("aem.satisfy.refreshing", false)

    /**
     * Satisfy handles plain OSGi bundle JAR's deployment by automatic wrapping to CRX package.
     * This path determines a path in JCR repository in which such bundles will be deployed on AEM.
     */
    @Input
    var satisfyBundlePath: String = propParser.string("aem.satisfy.bundlePath", "/apps/gradle-aem-plugin/satisfy/install")

    /**
     * A hook which could be used to override default properties used to generate a CRX package from OSGi bundle.
     */
    @Internal
    @get:JsonIgnore
    var satisfyBundleProperties: (Jar) -> Map<String, Any> = { mapOf() }

    /**
     * Determines which packages should be installed by default when satisfy task is being executed.
     */
    @Input
    var satisfyGroupName = propParser.string("aem.satisfy.group.name", "*")

    /**
     * Determines a Vault filter used to checkout JCR content from running AEM instance.
     *
     * @see <http://jackrabbit.apache.org/filevault/filter.html>
     *
     * Default: [automatically determined]
     */
    @Input
    var checkoutFilterPath: String = propParser.string("aem.checkout.filterPath", "")

    /**
     * Convention paths used to determine Vault checkout filter if it is not specified explicitly.
     *
     * Firstly there will be checked existence of 'checkout.xml' file.
     * By design, it should be customized version of 'filter.xml' with reduced count of filter roots
     * to avoid checking out too much content.
     *
     * As a fallback there will be used 'filter.xml' file. In that case same file will be used
     * to build CRX package and checkout JCR content from running instance.
     */
    @get:Internal
    @get:JsonIgnore
    val checkoutFilterPaths: List<String>
        get() = listOf("$vaultPath/checkout.xml", "$vaultPath/filter.xml")

    /**
     * Determines which files will be deleted within running cleaning
     * (e.g after checking out JCR content).
     */
    @Input
    var cleanFilesDeleted: MutableList<String> = mutableListOf(
            // VLT tool internal files
            "**/.vlt",
            "**/.vlt*.tmp",

            // Top level nodes should remain untouched
            "**/jcr_root/.content.xml",
            "**/jcr_root/apps/.content.xml",
            "**/jcr_root/conf/.content.xml",
            "**/jcr_root/content/.content.xml",
            "**/jcr_root/content/dam/.content.xml",
            "**/jcr_root/etc/.content.xml",
            "**/jcr_root/etc/designs/.content.xml",
            "**/jcr_root/home/.content.xml",
            "**/jcr_root/home/groups/.content.xml",
            "**/jcr_root/home/users/.content.xml",
            "**/jcr_root/libs/.content.xml",
            "**/jcr_root/system/.content.xml",
            "**/jcr_root/tmp/.content.xml",
            "**/jcr_root/var/.content.xml"
    )

    /**
     * Define here properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    @Input
    var cleanSkipProperties: MutableList<String> = mutableListOf(
            "jcr:uuid!**/home/users/*,**/home/groups/*",
            "jcr:lastModified",
            "jcr:created",
            "cq:lastModified*",
            "cq:lastReplicat*",
            "*_x0040_Delete",
            "*_x0040_TypeHint"
    )

    /**
     * Turn on/off default system notifications.
     */
    @Internal
    var notifications: Boolean = propParser.boolean("aem.notifications", false)

    /**
     * Initialize defaults that depends on concrete type of project.
     */
    init {
        project.afterEvaluate { validate() }
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun localInstance(httpUrl: String) {
        localInstance(httpUrl, {})
    }

    fun localInstance(httpUrl: String, configurer: LocalInstance.() -> Unit) {
        instance(LocalInstance.create(httpUrl, configurer))
    }

    fun localInstance(httpUrl: String, configurer: Closure<*>) {
        localInstance(httpUrl, { ConfigureUtil.configure(configurer, this) })
    }

    fun localAuthorInstance() {
        localAuthorInstance({})
    }

    fun localAuthorInstance(configurer: LocalInstance.() -> Unit) {
        localInstance(propParser.string(Instance.AUTHOR_URL_PROP, Instance.URL_AUTHOR_DEFAULT), configurer)
    }

    fun localAuthorInstance(configurer: Closure<*>) {
        localAuthorInstance({ ConfigureUtil.configure(configurer, this) })
    }

    fun localPublishInstance() {
        localPublishInstance({})
    }

    fun localPublishInstance(configurer: LocalInstance.() -> Unit) {
        localInstance(propParser.string(Instance.PUBLISH_URL_PROP, Instance.URL_PUBLISH_DEFAULT), configurer)
    }

    fun localPublishInstance(configurer: Closure<*>) {
        localPublishInstance({ ConfigureUtil.configure(configurer, this) })
    }

    fun remoteInstance(httpUrl: String) {
        remoteInstance(httpUrl, {})
    }

    fun remoteInstance(httpUrl: String, configurer: RemoteInstance.() -> Unit) {
        instance(RemoteInstance.create(httpUrl, {
            this.environment = this@AemConfig.environment
            this.apply(configurer)
        }))
    }

    fun remoteInstance(httpUrl: String, configurer: Closure<*>) {
        remoteInstance(httpUrl, { ConfigureUtil.configure(configurer, this) })
    }

    fun remoteAuthorInstance() {
        remoteAuthorInstance({})
    }

    fun remoteAuthorInstance(configurer: RemoteInstance.() -> Unit) {
        remoteInstance(propParser.string(Instance.AUTHOR_URL_PROP, Instance.URL_AUTHOR_DEFAULT), configurer)
    }

    fun remoteAuthorInstance(configurer: Closure<*>) {
        remoteAuthorInstance({ ConfigureUtil.configure(configurer, this) })
    }

    fun remotePublishInstance() {
        remotePublishInstance({})
    }

    fun remotePublishInstance(configurer: RemoteInstance.() -> Unit) {
        remoteInstance(propParser.string(Instance.PUBLISH_URL_PROP, Instance.URL_PUBLISH_DEFAULT), configurer)
    }

    fun remotePublishInstance(configurer: Closure<*>) {
        remotePublishInstance({ ConfigureUtil.configure(configurer, this) })
    }

    private fun instance(instance: Instance) {
        if (instances.containsKey(instance.name)) {
            throw AemException("Instance named '${instance.name}' is already defined. Enumerate instance types (for instance 'author1', 'author2') or distinguish environments.")
        }

        instances[instance.name] = instance
    }

    /**
     * Following checks will be performed during configuration phase.
     */
    fun validate() {
        if (bundlePath.isBlank()) {
            throw AemException("Bundle path cannot be blank")
        }

        if (contentPath.isBlank()) {
            throw AemException("Content path cannot be blank")
        }

        if (awaitStableTimeout >= awaitStableInterval) {
            throw AemException("Await timeout should be less than interval ($awaitStableTimeout < $awaitStableInterval)")
        }

        instances.values.forEach { it.validate() }
    }

    /**
     * CRX package Vault files will be composed from given sources.
     * Missing files required by package within installation will be auto-generated if 'vaultCopyMissingFiles' is enabled.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultFilesDirs: List<File>
        get() {
            val paths = listOf(
                    vaultFilesPath,
                    "$contentPath/${PackagePlugin.VLT_PATH}"
            )

            return paths.filter { !it.isBlank() }.map { File(it) }.filter { it.exists() }
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

    @get:Internal
    @get:JsonIgnore
    val vaultLineSeparatorString: String = LineSeparator.string(vaultLineSeparator)

    @Internal
    fun namePrefix(): String = if (isUniqueProjectName()) {
        project.name
    } else {
        "${project.rootProject.name}${project.path}"
                .replace(":", "-")
                .replace(".", "-")
                .substringBeforeLast("-")
    }

    @Internal
    @JsonIgnore
    fun isUniqueProjectName() = project == project.rootProject || project.name == project.rootProject.name

    companion object {

        /**
         * Shorthand getter for configuration related with specified project.
         * Especially useful when including one project in another (composing assembly packages).
         */
        fun of(project: Project): AemConfig {
            val extension = project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException(project.toString().capitalize()
                            + " has neither '${PackagePlugin.ID}' nor '${InstancePlugin.ID}' plugin applied.")

            return extension.config
        }

        fun of(task: DefaultTask): AemConfig {
            return of(task.project)
        }

        fun pkg(project: Project): ComposeTask {
            val task = project.tasks.findByName(ComposeTask.NAME)
                    ?: throw AemException("${project.toString().capitalize()} has no task named"
                            + " '${ComposeTask.NAME}' defined.")

            return task as ComposeTask
        }

        fun pkgs(project: Project): List<ComposeTask> {
            return project.allprojects.mapNotNull {
                if (it.plugins.hasPlugin(PackagePlugin.ID)) {
                    (it.tasks.getByName(ComposeTask.NAME) as ComposeTask)
                } else null
            }
        }

    }

}