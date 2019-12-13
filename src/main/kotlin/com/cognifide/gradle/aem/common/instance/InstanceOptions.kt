package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.Serializable

open class InstanceOptions(private val aem: AemExtension) : Serializable {

    private val definedCustom: MutableMap<String, Instance> = mutableMapOf()

    /**
     * List of AEM instances e.g on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    val defined = definedCustom.ifEmpty {
        mutableMapOf<String, Instance>().apply {
            Instance.defaultAuthor(aem).let { put(it.name, it) }
            Instance.defaultPublish(aem).let { put(it.name, it) }
        }
    }

    val definedList get() = defined.values.toList()

    /**
     * Customize default options for instance services.
     */
    fun sync(options: InstanceSync.() -> Unit) {
        syncOptions = options
    }

    @get:JsonIgnore
    internal var syncOptions: InstanceSync.() -> Unit = {}

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
        return definedCustom[urlOrName] ?: Instance.parse(aem, urlOrName).ifEmpty {
            throw AemException("Instance cannot be determined by value '$urlOrName'.")
        }.single().apply { validate() }
    }

    private fun define(instances: Iterable<Instance>) {
        instances.forEach { define(it) }
    }

    private fun define(instance: Instance) {
        if (definedCustom.containsKey(instance.name)) {
            throw AemException("Instance named '${instance.name}' is already defined. " +
                    "Enumerate instance types (for example 'author1', 'author2') " +
                    "or distinguish environments (for example 'local', 'int', 'stg').")
        }

        definedCustom[instance.name] = instance
    }

    init {
        // Define through command line
        val instancesForced = aem.prop.string("instance.list") ?: ""
        if (instancesForced.isNotBlank()) {
            define(Instance.parse(aem, instancesForced) { environment = Instance.ENVIRONMENT_CMD })
        }

        // Define through properties ]
        define(Instance.properties(aem))

        aem.project.afterEvaluate { _ ->
            definedCustom.values.forEach { it.validate() }
        }
    }
}
