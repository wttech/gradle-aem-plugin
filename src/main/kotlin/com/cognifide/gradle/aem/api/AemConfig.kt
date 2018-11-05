package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceType
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.RemoteInstance
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.LineSeparator
import com.cognifide.gradle.aem.internal.notifier.Notifier
import com.cognifide.gradle.aem.pkg.ComposeTask
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.Serializable

/**
 * Aggregated collection of AEM related configuration.
 *
 * Content paths which are used to compose a CRX package are being processed by copy task,
 * which automatically mark them as inputs so package is being rebuild on any JCR content or Vault files change.
 */
class AemConfig(
        @Transient
        @JsonIgnore
        private val aem: AemExtension,

        @Transient
        @JsonIgnore
        private val project: Project
) : Serializable {

    /**
     * Project name convention prefixes used to determine default:
     *
     * - bundle install subdirectory
     * - CRX package base name
     * - OSGi bundle JAR base name
     *
     * in case of multi-project build and assembly packages.
     */
    @get:Internal
    @get:JsonIgnore
    val projectPrefixes: MutableList<String> = mutableListOf("aem.", "aem-", "aem_")

    /**
     * Project name with skipped convention prefixes.
     */
    @get:Internal
    @get:JsonIgnore
    val projectName: String
        get() = project.name.run {
            var n = this; projectPrefixes.forEach { n = n.removePrefix(it) }; n
        }

    /**
     * Base name used as default for CRX packages being created by compose or collect task
     * and also for OSGi bundle JARs.
     */
    @get:Internal
    @get:JsonIgnore
    val baseName: String
        get() = Formats.normalizeSeparators(if (project == project.rootProject) {
            project.rootProject.name
        } else {
            "${project.rootProject.name}.$projectName"
        }, ".")

    /**
     * Determines current environment to be used in e.g package deployment.
     */
    @Input
    val environment: String = aem.props.string("aem.env") {
        System.getenv("AEM_ENV") ?: "local"
    }

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Input
    var instances: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Path in which local AEM instances will be stored.
     *
     * Default: "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
     */
    @Input
    var instanceRoot: String = "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"

    /**
     * Determines instances involved in CRX package deployment (filters preconfigured instances).
     */
    @Input
    var instanceName: String = aem.props.string("aem.instance.name", "$environment-*")

    /**
     * Forces instances involved in e.g CRX package deployment (uses explicit instances configuration).
     */
    @Input
    var instanceList: String = aem.props.string("aem.instance.list", "")

    /**
     * Determines instance which will be used when CRX package activation from author to publishers
     * will be performed (only if distributed deploy is enabled).
     */
    @Input
    var instanceAuthorName: String = aem.props.string("aem.instance.author.name", "$environment-${InstanceType.AUTHOR.type}*")

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     *
     * Default value may look quite big, but it is just very fail-safe.
     */
    @Input
    var instanceConnectionTimeout: Int = aem.props.int("aem.instance.connectionTimeout", 30000)

    /**
     * Determines if connection to untrusted (e.g. self-signed) SSL certificates should be allowed.
     *
     * By default allows all SSL connections.
     */
    @Input
    var instanceConnectionUntrustedSsl: Boolean = aem.props.boolean("aem.instance.connectionUntrustedSsl", true)

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    @Input
    var packageSnapshots: List<String> = aem.props.list("aem.package.snapshots")

    @Input
    var packageRoot: String = "${project.file("src/main/content")}"

    @get:Internal
    @get:JsonIgnore
    val packageJcrRoot: String
        get() = "$packageRoot/${PackagePlugin.JCR_ROOT}"

    @get:Internal
    @get:JsonIgnore
    val packageVltRoot: String
        get() = "$packageRoot/${PackagePlugin.VLT_PATH}"

    /**
     * Custom path to Vault files that will be used to build CRX package.
     * Useful to share same files for all packages, like package thumbnail.
     * Must be absolute or relative to current working directory.
     */
    @Input
    var vaultFilesPath: String = project.rootProject.file("src/main/resources/${PackagePlugin.VLT_PATH}").toString()

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    @Input
    var vaultLineSeparator: String = aem.props.string("aem.vlt.lineSeparator", "LF")

    /**
     * Turn on/off default system notifications.
     */
    @Internal
    var notificationEnabled: Boolean = aem.props.flag("aem.notification.enabled")

    /**
     * Hook for customizing notifications being displayed.
     *
     * To customize notification use one of concrete provider methods: 'dorkbox' or 'jcgay' (and optionally pass configuration lambda(s)).
     * Also it is possible to implement own notifier directly in build script by using provider method 'custom'.
     */
    @Internal
    @JsonIgnore
    var notificationConfig: (AemNotifier) -> Notifier = { it.factory() }

    /**
     * Initialize defaults that depends on concrete type of project then validate configuration.
     */
    init {
        project.afterEvaluate {
            defaults()
            validate()
        }
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun localInstance(httpUrl: String) {
        localInstance(httpUrl) {}
    }

    fun localInstance(httpUrl: String, configurer: LocalInstance.() -> Unit) {
        instance(LocalInstance.create(project, httpUrl) {
            this.environment = this@AemConfig.environment
            this.apply(configurer)
        })
    }

    fun remoteInstance(httpUrl: String) {
        remoteInstance(httpUrl) {}
    }

    fun remoteInstance(httpUrl: String, configurer: RemoteInstance.() -> Unit) {
        instance(RemoteInstance.create(project, httpUrl) {
            this.environment = this@AemConfig.environment
            this.apply(configurer)
        })
    }

    fun parseInstance(urlOrName: String): Instance {
        return instances[urlOrName]
                ?: Instance.parse(project, urlOrName).single().apply { validate() }
    }

    private fun instances(instances: Collection<Instance>) {
        instances.forEach { instance(it) }
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
        instances.values.forEach { it.validate() }
    }

    @get:Internal
    @get:JsonIgnore
    val vaultLineSeparatorString: String = LineSeparator.string(vaultLineSeparator)

    private fun defaults() {
        // Define through command line (forced instances)
        if (instanceList.isNotBlank()) {
            instances(Instance.parse(project, instanceList))
        }

        // Define through properties
        instances(Instance.properties(project))

        // Define defaults if still no instances defined at all
        if (instances.isEmpty()) {
            instances(Instance.defaults(project))
        }
    }

    companion object {

        // TODO to be removed
        fun pkgs(project: Project): List<ComposeTask> {
            return project.allprojects.mapNotNull {
                if (it.plugins.hasPlugin(PackagePlugin.ID)) {
                    (it.tasks.getByName(ComposeTask.NAME) as ComposeTask)
                } else null
            }
        }

    }

}