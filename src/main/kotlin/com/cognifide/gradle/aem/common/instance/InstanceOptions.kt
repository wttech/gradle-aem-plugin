package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

open class InstanceOptions(private val aem: AemExtension) : Serializable {

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    val defined: MutableMap<String, Instance> = mutableMapOf()

    /**
     * Customize HTTP connection to AEM instances.
     *
     * Allows to e.g
     * - set using HTTP proxy,
     * - customize maximum time after which initializing connection to AEM will be aborted (e.g on upload, install),
     * - customize any options offered by Apache HTTP Client builder (use its API directly).
     */
    fun http(options: InstanceHttpClient.(Instance) -> Unit) {
        httpOptions = options
    }

    @get:JsonIgnore
    internal var httpOptions: InstanceHttpClient.(Instance) -> Unit = {
        connectionTimeout = aem.props.int("instance.http.connectionTimeout") ?: 30000
        connectionRetries = aem.props.boolean("instance.http.connectionRetries") ?: true
        connectionIgnoreSsl = aem.props.boolean("instance.http.connectionIgnoreSsl") ?: true

        proxyHost = aem.props.string("instance.http.proxyHost")
        proxyPort = aem.props.int("instance.http.proxyPort")
        proxyScheme = aem.props.string("instance.http.proxyScheme")
    }

    /**
     * Allows to control automatic system properties (and product version) gathering from running instance to defined.
     * Essential for correctly working features that are using timestamps like instance tail and instance events check.
     */
    var statusProperties: Boolean = aem.props.boolean("instance.statusProperties") ?: !aem.offline

    /**
     * Allows to control automatic node types gathering from running instance to defined.
     * Essential for correctly working package validation.
     */
    var crxNodeTypes: Boolean = aem.props.boolean("instance.crxNodeTypes") ?: !aem.offline

    /**
     * Define local instance (created on local file system).
     */
    fun local(httpUrl: String) {
        local(httpUrl) {}
    }

    /**
     * Define local instance (created on local file system).
     */
    fun local(httpUrl: String, name: String) {
        local(httpUrl) { this.name = name }
    }

    /**
     * Define local instance (created on local file system).
     */
    fun local(httpUrl: String, options: LocalInstance.() -> Unit) {
        define(LocalInstance.create(aem, httpUrl, options))
    }

    /**
     * Define remote instance (available on any host).
     */
    fun remote(httpUrl: String) {
        remote(httpUrl) {}
    }

    /**
     * Define remote instance (available on any host).
     */
    fun remote(httpUrl: String, name: String) {
        remote(httpUrl) { this.name = name }
    }

    /**
     * Define remote instance (available on any host).
     */
    fun remote(httpUrl: String, options: RemoteInstance.() -> Unit) {
        define(RemoteInstance.create(aem, httpUrl, options))
    }

    /**
     * Get defined instance by name or create temporary definition if URL provided.
     */
    fun parse(urlOrName: String): Instance {
        return defined[urlOrName] ?: Instance.parse(aem, urlOrName).ifEmpty {
            throw AemException("Instance cannot be determined by value '$urlOrName'.")
        }.single().apply { validate() }
    }

    private fun define(instances: Iterable<Instance>) {
        instances.forEach { define(it) }
    }

    private fun define(instance: Instance) {
        if (defined.containsKey(instance.name)) {
            throw AemException("Instance named '${instance.name}' is already defined. " +
                    "Enumerate instance types (for example 'author1', 'author2') " +
                    "or distinguish environments (for example 'local', 'int', 'stg').")
        }

        defined[instance.name] = instance
    }

    init {
        // Define through command line
        val instancesForced = aem.props.string("instance.list") ?: ""
        if (instancesForced.isNotBlank()) {
            define(Instance.parse(aem, instancesForced) { environment = Instance.ENVIRONMENT_CMD })
        }

        // Define through properties ]
        define(Instance.properties(aem))

        aem.project.afterEvaluate { _ ->
            // Ensure defaults if still no instances defined at all
            if (defined.isEmpty()) {
                define(Instance.defaults(aem) { environment = aem.env })
            }

            // Validate all
            defined.values.forEach { it.validate() }
        }
    }
}
