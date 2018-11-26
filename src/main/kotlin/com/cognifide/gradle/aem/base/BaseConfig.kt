package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceHttpClient
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.RemoteInstance
import com.cognifide.gradle.aem.internal.LineSeparator
import com.cognifide.gradle.aem.internal.notifier.Notifier
import com.cognifide.gradle.aem.pkg.PackagePlugin
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

/**
 * General AEM related configuration (shared for tasks).
 */
class BaseConfig(
    @Transient
    @JsonIgnore
    private val aem: AemExtension
) : Serializable {

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Nested
    var instances: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Path in which local AEM instances will be stored.
     *
     * Default: "${System.getProperty("user.home")}/.aem/${project.rootProject.name}"
     */
    @Input
    var instanceRoot: String = "${System.getProperty("user.home")}/.aem/${aem.project.rootProject.name}"

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     *
     * Default value may look quite big, but it is just very fail-safe.
     */
    @Internal
    @JsonIgnore
    var instanceHttpOptions: (InstanceHttpClient).() -> Unit = {
        connectionTimeout = aem.props.int("aem.instance.httpOptions.connectionTimeout") ?: 30000
        connectionRetries = aem.props.boolean("aem.instance.httpOptions.connectionRetries") ?: true
        connectionIgnoreSsl = aem.props.boolean("aem.instance.httpOptions.connectionIgnoreSsl") ?: true
    }

    /**
     * CRX package name conventions (with wildcard) indicating that package can change over time
     * while having same version specified. Affects CRX packages composed and satisfied.
     */
    @Input
    var packageSnapshots: List<String> = aem.props.list("aem.package.snapshots") ?: listOf()

    @Input
    var packageRoot: String = "${aem.project.file("src/main/content")}"

    @get:Internal
    @get:JsonIgnore
    val packageJcrRoot: String
        get() = "$packageRoot/${PackagePlugin.JCR_ROOT}"

    @get:Internal
    @get:JsonIgnore
    val packageVltRoot: String
        get() = "$packageRoot/${PackagePlugin.VLT_PATH}"

    /**
     * Content path for OSGi bundle jars being placed in CRX package.
     *
     * Default convention assumes that subprojects have separate bundle paths, because of potential re-installation of subpackages.
     * When all subprojects will have same bundle path, reinstalling one subpackage may end with deletion of other bundles coming from another subpackage.
     *
     * Beware that more nested bundle install directories are not supported by AEM by default (up to 4th depth level).
     * That's the reason of using dots in subproject names to avoid that limitation.
     */
    @Input
    var packageInstallPath: String = if (aem.project == aem.project.rootProject) {
        "/apps/${aem.project.rootProject.name}/install"
    } else {
        "/apps/${aem.project.rootProject.name}/${aem.projectName}/install"
    }

    /**
     * Define known exceptions which could be thrown during package installation
     * making it impossible to succeed.
     *
     * When declared exception is encountered during package installation process, no more
     * retries will be applied.
     */
    @Input
    var packageErrors = aem.props.list("aem.package.errors") ?: listOf(
            "javax.jcr.nodetype.ConstraintViolationException",
            "org.apache.jackrabbit.vault.packaging.DependencyException",
            "org.xml.sax.SAXException"
    )

    /**
     * Determines number of lines to process at once during reading html responses.
     *
     * The higher the value, the bigger consumption of memory but shorter execution time.
     * It is a protection against exceeding max Java heap size.
     */
    @Input
    var packageResponseBuffer = aem.props.int("aem.package.responseBuffer") ?: 4096

    /**
     * Specify characters to be used as line endings when cleaning up checked out JCR content.
     */
    @Input
    var lineSeparator: String = aem.props.string("aem.lineSeparator") ?: "LF"

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
    var notificationConfig: (com.cognifide.gradle.aem.base.Notifier) -> Notifier = { it.factory() }

    init {
        // Define through command line
        val instancesForced = aem.props.string("aem.instances") ?: ""
        if (instancesForced.isNotBlank()) {
            instances(Instance.parse(aem.project, instancesForced))
        }

        // Define through properties ]
        instances(Instance.properties(aem.project))

        aem.project.afterEvaluate { _ ->
            // Ensure defaults if still no instances defined at all
            if (instances.isEmpty()) {
                instances(Instance.defaults(aem.project, aem.environment))
            }

            // Validate all
            instances.values.forEach { it.validate() }
        }
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun localInstance(httpUrl: String) {
        localInstance(httpUrl) {}
    }

    fun localInstance(httpUrl: String, configurer: LocalInstance.() -> Unit) {
        instance(LocalInstance.create(aem.project, httpUrl) {
            this.environment = aem.environment
            this.apply(configurer)
        })
    }

    fun remoteInstance(httpUrl: String) {
        remoteInstance(httpUrl) {}
    }

    fun remoteInstance(httpUrl: String, configurer: RemoteInstance.() -> Unit) {
        instance(RemoteInstance.create(aem.project, httpUrl) {
            this.environment = aem.environment
            this.apply(configurer)
        })
    }

    fun parseInstance(urlOrName: String): Instance {
        return instances[urlOrName]
                ?: Instance.parse(aem.project, urlOrName).single().apply { validate() }
    }

    private fun instances(instances: Collection<Instance>) {
        instances.forEach { instance(it) }
    }

    private fun instance(instance: Instance) {
        if (instances.containsKey(instance.name)) {
            throw AemException("Instance named '${instance.name}' is already defined. " +
                    "Enumerate instance types (for example 'author1', 'author2') " +
                    "or distinguish environments (for example 'local', 'int', 'stg').")
        }

        instances[instance.name] = instance
    }

    @get:Internal
    @get:JsonIgnore
    val lineSeparatorString: String = LineSeparator.string(lineSeparator)
}