package com.cognifide.gradle.aem

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.RemoteInstance
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.DefaultTask
import org.gradle.api.Incubating
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
 * Notice that this whole object is serializable and marked as input of tasks, so there is no need to mark
 * each property as input.
 *
 * What is more, content paths which are used to compose a CRX package are being processed by copy task,
 * which automatically mark them as inputs so package is being rebuild on any content change.
 */
open class AemConfig(project: Project) : Serializable {

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Input
    var instances: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     */
    @Input
    var deployConnectionTimeout: Int = 5000

    /**
     * Perform deploy action (upload, install or activate) in parallel to multiple instances at once.
     */
    @Incubating
    @Input
    var deployParallel: Boolean = false

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified.
     */
    @Input
    var deploySnapshots: List<String> = mutableListOf()

    /**
     * Force upload CRX package regardless if it was previously uploaded.
     */
    @Input
    var uploadForce: Boolean = true

    /**
     * Determines if when on package install, sub-packages included in CRX package content should be also installed.
     */
    @Input
    var recursiveInstall: Boolean = true

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
    var filesExpanded: MutableList<String> = mutableListOf("**/${AemPackagePlugin.VLT_PATH}/*.xml")

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
    var vaultFilesPath: String = "${project.rootProject.projectDir.path}/src/main/resources/${AemPackagePlugin.VLT_PATH}"

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
    var vaultGlobalOptions: String = "--credentials {{instance.credentials}}"

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    @Input
    var vaultLineSeparator: String = System.lineSeparator()

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
     * Build date used as base for calculating 'created' and 'buildCount' package properties.
     */
    @Internal
    var buildDate: Date = Date()

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
    var instanceFilesPath: String = "${project.rootProject.projectDir.path}/src/main/resources/${AemInstancePlugin.FILES_PATH}"

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    @Input
    var instanceFilesExpanded: MutableList<String> = mutableListOf("**/*.properties", "**/*.sh", "**/*.bat", "**/*.xml")

    /**
     * Time in milliseconds to postpone instance stability checks to avoid race condition related with
     * actual operation being performed on AEM like starting JCR package installation or even creating launchpad.
     */
    @Input
    var awaitDelay: Long = TimeUnit.SECONDS.toMillis(3)

    /**
     * Time in milliseconds used as interval between next instance stability checks being performed.
     * Optimization could be necessary only when instance is heavily loaded.
     */
    @Input
    var awaitInterval: Long = TimeUnit.SECONDS.toMillis(1)

    /**
     * After each await interval, instance stability check is being performed.
     * This value is a HTTP connection timeout (in millis) which must be smaller than interval to avoid race condition.
     */
    @Input
    var awaitTimeout: Int = (0.9 * awaitInterval.toDouble()).toInt()

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    @Input
    var awaitTimes: Long = 60 * 5

    /**
     * Satisfy is a lazy task, which means that it will not install package that is already installed.
     * By default, information about currently installed packages is being retrieved from AEM only once.
     *
     * This flag can change that behavior, so that information will be refreshed after each package installation.
     */
    @Incubating
    @Input
    var satisfyRefreshing: Boolean = false

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
        instance(LocalInstance(httpUrl))
    }

    fun localInstance(httpUrl: String, password: String) {
        instance(LocalInstance(httpUrl, password))
    }

    fun localInstance(httpUrl: String, user: String, password: String) {
        instance(LocalInstance(httpUrl, user, password))
    }

    fun localInstance(httpUrl: String, user: String, password: String, type: String, debugPort: Int) {
        instance(LocalInstance(httpUrl, user, password, type, debugPort))
    }

    fun localInstance(httpUrl: String, user: String, password: String, type: String, debugPort: Int, jvmOpts: List<String>, startOpts: List<String>) {
        instance(LocalInstance(httpUrl, user, password, type, debugPort, jvmOpts, startOpts))
    }

    fun remoteInstance(httpUrl: String) {
        instance(RemoteInstance(httpUrl))
    }

    fun remoteInstance(httpUrl: String, environment: String) {
        instance(RemoteInstance(httpUrl, environment))
    }

    fun remoteInstance(httpUrl: String, user: String, password: String, type: String, environment: String) {
        instance(RemoteInstance(httpUrl, user, password, type, environment))
    }

    private fun instance(instance: Instance) {
        instances.put(instance.name, instance)
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
                    "$contentPath/${AemPackagePlugin.VLT_PATH}"
            )

            return paths.filter { !it.isBlank() }.map { File(it) }.filter { it.exists() }
        }

    /**
     * CRX package Vault files path.
     */
    @get:Internal
    @get:JsonIgnore
    val vaultPath: String
        get() = "$contentPath/${AemPackagePlugin.VLT_PATH}"

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

    companion object {

        /**
         * Shorthand getter for configuration related with specified project.
         * Especially useful when including one project in another (composing assembly packages).
         */
        fun of(project: Project): AemConfig {
            val extension = project.extensions.findByType(AemExtension::class.java)
                    ?: throw AemException(project.toString().capitalize()
                    + " has neither '${AemPackagePlugin.ID}' nor '${AemInstancePlugin.ID}' plugin applied.")

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
                if (it.plugins.hasPlugin(AemPackagePlugin.ID)) {
                    (it.tasks.getByName(ComposeTask.NAME) as ComposeTask)
                } else null
            }
        }

    }

}