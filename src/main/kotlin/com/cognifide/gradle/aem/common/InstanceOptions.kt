package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.instance.*
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

open class InstanceOptions(private val aem: AemExtension) : Serializable {

    private val defined: MutableMap<String, Instance> = mutableMapOf()

    /**
     * List of AEM instances on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    @Nested
    var instances: Map<String, Instance> = defined.toMap()

    /**
     * Defines maximum time after which initializing connection to AEM will be aborted (e.g on upload, install).
     *
     * Default value may look quite big, but it is just very fail-safe.
     */
    @Internal
    @JsonIgnore
    var instanceHttpOptions: (InstanceHttpClient).() -> Unit = {
        connectionTimeout = aem.props.int("instanceHttpOptions.connectionTimeout") ?: 30000
        connectionRetries = aem.props.boolean("instanceHttpOptions.connectionRetries") ?: true
        connectionIgnoreSsl = aem.props.boolean("instanceHttpOptions.connectionIgnoreSsl") ?: true
    }

    /**
     * Declare new deployment target (AEM instance).
     */
    fun localInstance(httpUrl: String) {
        localInstance(httpUrl) {}
    }

    fun localInstance(httpUrl: String, configurer: LocalInstance.() -> Unit) {
        instance(LocalInstance.create(aem, httpUrl, configurer))
    }

    fun remoteInstance(httpUrl: String) {
        remoteInstance(httpUrl) {}
    }

    fun remoteInstance(httpUrl: String, configurer: RemoteInstance.() -> Unit) {
        instance(RemoteInstance.create(aem, httpUrl, configurer))
    }

    fun parseInstance(urlOrName: String): Instance {
        return instances[urlOrName] ?: Instance.parse(aem, urlOrName).ifEmpty {
            throw AemException("Instance cannot be determined by value '$urlOrName'.")
        }.single().apply { validate() }
    }

    private fun instances(instances: Collection<Instance>) {
        instances.forEach { instance(it) }
    }

    private fun instance(instance: Instance) {
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
            instances(Instance.parse(aem, instancesForced) { environment = Instance.ENVIRONMENT_CMD })
        }

        // Define through properties ]
        instances(Instance.properties(aem))

        aem.project.afterEvaluate { _ ->
            // Ensure defaults if still no instances defined at all
            if (instances.isEmpty()) {
                instances(Instance.defaults(aem) { environment = aem.env })
            }

            // Validate all
            instances.values.forEach { it.validate() }
        }
    }
}