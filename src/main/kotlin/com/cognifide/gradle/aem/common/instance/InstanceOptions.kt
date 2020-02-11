package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension

open class InstanceOptions(private val aem: AemExtension) {

    /**
     * Directory storing instance wide configuration files.
     */
    val configDir = aem.obj.dir {
        convention(aem.obj.projectDir("src/aem/instance"))
        aem.prop.file("instance.configDir")?.let { set(it) }
    }

    /**
     * List of AEM instances e.g on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    val defined = aem.obj.list<Instance> {
        convention(aem.obj.provider {
            val fromCmd = aem.prop.string("instance.list")?.let {
                Instance.parse(aem, it) { environment = Instance.ENVIRONMENT_CMD }
            } ?: listOf()
            val fromProperties = Instance.properties(aem)

            (fromCmd + fromProperties).ifEmpty {
                listOf(
                        Instance.defaultAuthor(aem),
                        Instance.defaultPublish(aem)
                )
            }
        })
    }

    /**
     * Map of AEM instances with names as a keys.
     */
    val all = defined.map { p -> p.map { it.name to it }.toMap() }

    /**
     * Customize default options for instance services.
     */
    fun sync(options: InstanceSync.() -> Unit) {
        syncOptions = options
    }

    internal var syncOptions: InstanceSync.() -> Unit = {}

    /**
     * Define local instance (created on local file system).
     */
    fun local(httpUrl: String) = local(httpUrl) {}

    /**
     * Define local instance (created on local file system).
     */
    fun local(httpUrl: String, name: String) = local(httpUrl) { this.name = name }

    /**
     * Define local instance (created on local file system).
     */
    fun local(httpUrl: String, options: LocalInstance.() -> Unit) {
        defined.add(aem.obj.provider { LocalInstance.create(aem, httpUrl, options) })
    }

    /**
     * Define remote instance (available on any host).
     */
    fun remote(httpUrl: String) = remote(httpUrl) {}

    /**
     * Define remote instance (available on any host).
     */
    fun remote(httpUrl: String, name: String) = remote(httpUrl) { this.name = name }

    /**
     * Define remote instance (available on any host).
     */
    fun remote(httpUrl: String, options: RemoteInstance.() -> Unit) {
        defined.add(aem.obj.provider { RemoteInstance.create(aem, httpUrl, options) })
    }

    fun named(name: String) = defined.get().firstOrNull()
            ?: throw InstanceException("Instance named '$name' is not defined!")

    /**
     * Get defined instance by name or create temporary definition if URL provided.
     */
    fun parse(url: String): Instance = Instance.parse(aem, url).ifEmpty {
        throw InstanceException("Instance URL cannot be parsed properly '$url'!")
    }.single().apply { validate() }
}
