package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.*
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.satisfy.Satisfier
import com.cognifide.gradle.aem.common.instance.tail.Tailer
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.common.pluginProject
import com.cognifide.gradle.common.utils.using

open class InstanceManager(val aem: AemExtension) {

    private val project = aem.project

    /**
     * Using remote AEM instances is acceptable in any project, so that lookup for project applying local instance plugin is required
     * Needed to determine common directory storing instance related resources (tailer incident filter, Groovy scripts etc).
     */
    val projectDir = aem.obj.dir {
        convention(aem.obj.provider {
            project.pluginProject(InstancePlugin.ID)?.layout?.projectDirectory ?: throw InstanceException(
                    "Using remote AEM instances requires having at least one project applying plugin '${InstancePlugin.ID}'" +
                    " or setting property 'instance.projectDir'!"
            )
        })
        aem.prop.string("instance.projectDir")?.let { set(project.rootProject.file(it)) }
    }

    /**
     * Directory storing instance wide configuration files.
     */
    val configDir = aem.obj.dir {
        convention(projectDir.dir("src/aem/instance"))
        aem.prop.file("instance.configDir")?.let { set(it) }
    }

    /**
     * Directory storing outputs of instance tasks.
     */
    val buildDir = aem.obj.dir {
        convention(projectDir.dir("build/instance"))
        aem.prop.file("instance.buildDir")?.let { set(it) }
    }

    val satisfier by lazy { Satisfier(this) }

    fun satisfier(options: Satisfier.() -> Unit) = satisfier.using(options)

    val provisioner by lazy { Provisioner(this) }

    fun provisioner(options: Provisioner.() -> Unit) = provisioner.using(options)

    val tailer by lazy { Tailer(this) }

    fun tailer(options: Tailer.() -> Unit) = tailer.using(options)

    fun resolveFiles() {
        satisfier.resolve()
    }

    // ===== Definition API =====

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
            (fromCmd + fromProperties).ifEmpty { Instance.defaultPair(aem) }
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
    fun remote(httpUrl: String, options: Instance.() -> Unit) {
        defined.add(aem.obj.provider { Instance.create(aem, httpUrl, options) })
    }

    fun named(name: String) = defined.get().firstOrNull()
            ?: throw InstanceException("Instance named '$name' is not defined!")

    /**
     * Get defined instance by name or create temporary definition if URL provided.
     */
    fun parse(url: String): Instance = Instance.parse(aem, url).ifEmpty {
        throw InstanceException("Instance URL cannot be parsed properly '$url'!")
    }.single()

    // ===== Actions API =====

    fun awaitUp(instance: Instance, options: AwaitUpAction.() -> Unit = {}) = awaitUp(listOf(instance), options)

    fun awaitUp(instances: Collection<Instance> = aem.instances, options: AwaitUpAction.() -> Unit = {}) {
        AwaitUpAction(aem).apply(options).perform(instances)
    }

    fun awaitDown(instance: Instance, options: AwaitDownAction.() -> Unit = {}) = awaitDown(listOf(instance), options)

    fun awaitDown(instances: Collection<Instance> = aem.instances, options: AwaitDownAction.() -> Unit = {}) {
        AwaitDownAction(aem).apply(options).perform(instances)
    }

    fun awaitReloaded(instance: Instance, reloadOptions: ReloadAction.() -> Unit = {}, awaitUpOptions: AwaitUpAction.() -> Unit = {}) {
        awaitReloaded(listOf(instance), reloadOptions, awaitUpOptions)
    }

    fun awaitReloaded(
        instances: Collection<Instance> = aem.instances,
        reloadOptions: ReloadAction.() -> Unit = {},
        awaitUpOptions: AwaitUpAction.() -> Unit = {}
    ) {
        reload(instances, reloadOptions)
        awaitUp(instances, awaitUpOptions)
    }

    fun reload(instance: Instance, options: ReloadAction.() -> Unit = {}) = reload(listOf(instance), options)

    fun reload(instances: Collection<Instance> = aem.instances, options: ReloadAction.() -> Unit = {}) = ReloadAction(aem).apply(options).perform(instances)

    fun check(instance: Instance, options: CheckAction.() -> Unit) = check(listOf(instance), options)

    fun check(instances: Collection<Instance> = aem.instances, options: CheckAction.() -> Unit) = CheckAction(aem).apply(options).perform(instances)

    fun examine(instance: Instance) = examine(listOf(instance))

    fun examine(instances: Collection<Instance> = aem.instances) {
        examineAvailable(instances)
        examineRunningOther(instances)
    }

    fun examineAvailable(instances: Collection<Instance> = aem.instances) {
        val unavailable = instances.filter { !it.available }
        if (unavailable.isNotEmpty()) {
            throw InstanceException("Instances are unavailable (${unavailable.size}):\n" +
                    unavailable.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}'" } + "\n\n" +
                    "Ensure having correct URLs defined, credentials correctly encoded and networking in correct state (internet accessible, VPN on/off)"
            )
        }
    }

    fun examineRunningOther(instances: Collection<Instance>) {
        val running = instances.filterIsInstance<LocalInstance>().filter { it.runningOther }
        if (running.isNotEmpty()) {
            throw InstanceException("Instances are already running (${running.size}):\n" +
                    running.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}' located at path '${it.runningDir}'" } + "\n\n" +
                    "Ensure having these instances down."
            )
        }
    }
}
