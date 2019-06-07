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
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     *
     * Default value may look quite big, but it is just very fail-safe.
     */
    @JsonIgnore
    var httpOptions: (InstanceHttpClient).() -> Unit = {
        connectionTimeout = aem.props.int("instance.http.connectionTimeout") ?: 30000
        connectionRetries = aem.props.boolean("instance.http.connectionRetries") ?: true
        connectionIgnoreSsl = aem.props.boolean("instance.http.connectionIgnoreSsl") ?: true
    }

    /**
     * Allows to control automatic system properties (and product version) gathering from running instance to instances defined.
     * Essential for correctly working features that are using timestamps like instance tail and instance events check.
     */
    var statusProperties: Boolean = aem.props.boolean("instance.statusProperties")
            ?: !aem.project.gradle.startParameter.isOffline

    /**
     * Declare new deployment target (AEM instance).
     */
    fun local(httpUrl: String) {
        local(httpUrl) {}
    }

    fun local(httpUrl: String, configurer: LocalInstance.() -> Unit) {
        define(LocalInstance.create(aem, httpUrl, configurer))
    }

    fun remote(httpUrl: String) {
        remote(httpUrl) {}
    }

    fun remote(httpUrl: String, configurer: RemoteInstance.() -> Unit) {
        define(RemoteInstance.create(aem, httpUrl, configurer))
    }

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