package com.cognifide.gradle.aem.api

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.LineSeparator
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
import java.io.Serializable
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Aggregated collection of AEM related configuration.
 *
 * Content paths which are used to compose a CRX package are being processed by copy task,
 * which automatically mark them as inputs so package is being rebuild on any JCR content or Vault files change.
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
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Input
    var instances: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Determines current environment to be used in deployment.
     */
    @Input
    val deployEnvironment: String = propParser.string("aem.env", {
        System.getenv("AEM_ENV") ?: "local"
    })

    /**
     * Determines instances involved in CRX package deployment (filters preconfigured instances).
     */
    @Input
    var deployInstanceName: String = propParser.string("aem.deploy.instance.name", "$deployEnvironment-*")

    /**
     * Forces instances involved in CRX package deployment (uses explicit instances configuration).
     */
    @Input
    var deployInstanceList: String = propParser.string("aem.deploy.instance.list", "")

    /**
     * Determines instance which will be used when:
     *
     * - CRX package activation from author to publishers will be performed (only if distributed deploy is enabled).
     * - Task 'aemCheckout' or 'aemSync' will be executed.
     */
    @Input
    var deployInstanceAuthorName: String = "$deployEnvironment-${InstanceType.AUTHOR.type}"

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     */
    @Input
    var deployConnectionTimeout: Int = propParser.int("aem.deploy.connectionTimeout", 5000)

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
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var installRecursive: Boolean = propParser.boolean("aem.install.recursive", true)

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
    var contentPath: String = "${project.projectDir.path}/src/main/content"

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
     * Define here properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    @Input
    var vaultSkipProperties: MutableList<String> = mutableListOf(
            "jcr:uuid!**/home/users/*,**/home/groups/*",
            "jcr:lastModified",
            "jcr:created",
            "cq:lastModified*",
            "cq:lastReplicat*",
            "*_x0040_Delete",
            "*_x0040_TypeHint"
    )

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
     * Default: "${System.getProperty("user.home")}/.gradle/aem/${project.rootProject.name}"
     */
    @Input
    var instancesPath: String = "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    @Input
    var instanceFilesPath: String = project.rootProject.file("src/main/resources/${InstancePlugin.FILES_PATH}").toString()

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    @Input
    var instanceFilesExpanded: MutableList<String> = mutableListOf("**/*.properties", "**/*.sh", "**/*.bat", "**/*.xml", "**/start", "**/stop")

    /**
     * Time in milliseconds to postpone instance stability checks to avoid race condition related with
     * actual operation being performed on AEM like starting JCR package installation or even creating launchpad.
     */
    @Input
    var awaitDelay: Long = propParser.long("aem.await.delay", TimeUnit.SECONDS.toMillis(3))

    /**
     * Time in milliseconds used as interval between next instance stability checks being performed.
     * Optimization could be necessary only when instance is heavily loaded.
     */
    @Input
    var awaitInterval: Long = propParser.long("aem.await.interval", TimeUnit.SECONDS.toMillis(1))

    /**
     * After each await interval, instance stability check is being performed.
     * This value is a HTTP connection timeout (in millis) which must be smaller than interval to avoid race condition.
     */
    @Input
    var awaitTimeout: Int = propParser.int("aem.await.timeout", (0.9 * awaitInterval.toDouble()).toInt())

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    @Input
    var awaitTimes: Long = propParser.long("aem.await.times", 60 * 5)

    /**
     * If there is still some unstable instance left, then fail build except just logging warning.
     */
    @Input
    var awaitFail: Boolean = propParser.boolean("aem.await.fail", true)

    /**
     * Number of intervals / additional instance stability checks to assure all stable instances.
     */
    @Input
    var awaitAssurances: Long = propParser.long("aem.await.assurances", 1L)

    /**
     * Hook for customizing condition being an instance stability check.
     */

    @Internal
    @get:JsonIgnore
    var awaitCondition: (InstanceState) -> Boolean = { it.stable }

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
     * Initialize defaults that depends on concrete type of project.
     */
    init {
        project.afterEvaluate { validate() }
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun localInstance(httpUrl: String) {
        instance(LocalInstance.create(httpUrl))
    }

    fun localInstance(httpUrl: String, type: String) {
        instance(LocalInstance.create(httpUrl, type))
    }

    fun localInstance(httpUrl: String, user: String, password: String) {
        instance(LocalInstance.create(httpUrl, user, password))
    }

    fun localInstance(httpUrl: String, user: String, password: String, type: String) {
        instance(LocalInstance.create(httpUrl, user, password, type))
    }

    fun localInstance(httpUrl: String, user: String, password: String, type: String, debugPort: Int) {
        instance(LocalInstance(httpUrl, user, password, type, debugPort))
    }

    fun localAuthorInstance() {
        val httpUrl = propParser.string(Instance.AUTHOR_URL_PROP, Instance.URL_AUTHOR_DEFAULT)
        instance(LocalInstance.create(httpUrl))
    }

    fun localPublishInstance() {
        val httpUrl = propParser.string(Instance.PUBLISH_URL_PROP, Instance.URL_PUBLISH_DEFAULT)
        instance(LocalInstance.create(httpUrl))
    }

    fun remoteInstance(httpUrl: String) {
        instance(RemoteInstance.create(httpUrl, deployEnvironment))
    }

    fun remoteInstance(httpUrl: String, environment: String) {
        instance(RemoteInstance.create(httpUrl, environment))
    }

    fun remoteInstance(httpUrl: String, user: String, password: String, environment: String) {
        instance(RemoteInstance.create(httpUrl, user, password, environment))
    }

    fun remoteInstance(httpUrl: String, user: String, password: String, type: String, environment: String) {
        instance(RemoteInstance(httpUrl, user, password, type, environment))
    }

    fun remoteAuthorInstance() {
        val httpUrl = propParser.string(Instance.AUTHOR_URL_PROP, Instance.URL_AUTHOR_DEFAULT)
        instance(RemoteInstance.create(httpUrl, deployEnvironment))
    }

    fun remotePublishInstance() {
        val httpUrl = propParser.string(Instance.PUBLISH_URL_PROP, Instance.URL_PUBLISH_DEFAULT)
        instance(RemoteInstance.create(httpUrl, deployEnvironment))
    }

    private fun instance(instance: Instance) {
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

        if (awaitTimeout >= awaitInterval) {
            throw AemException("Await timeout should be less than interval ($awaitTimeout < $awaitInterval)")
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
     * Also used by VLT tool as default filter for files being checked out from running AEM instance.
     *
     * @see <http://jackrabbit.apache.org/filevault/filter.html>
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