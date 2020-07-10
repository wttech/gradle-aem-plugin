package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.action.*
import com.cognifide.gradle.sling.common.instance.provision.Provisioner
import com.cognifide.gradle.sling.common.instance.tail.Tailer
import com.cognifide.gradle.sling.instance.InstancePlugin
import com.cognifide.gradle.common.pluginProject
import com.cognifide.gradle.common.utils.using

open class InstanceManager(val sling: SlingExtension) {

    private val project = sling.project

    private val logger = project.logger

    val local by lazy { sling.localInstanceManager }

    /**
     * Using remote Sling instances is acceptable in any project, so that lookup for project applying local instance plugin is required
     * Needed to determine common directory storing instance related resources (tailer incident filter).
     */
    val projectDir = sling.obj.dir {
        convention(sling.obj.provider {
            project.pluginProject(InstancePlugin.ID)?.layout?.projectDirectory ?: throw InstanceException(
                    "Using remote Sling instances requires having at least one project applying plugin '${InstancePlugin.ID}'" +
                    " or setting property 'instance.projectDir'!"
            )
        })
        sling.prop.string("instance.projectDir")?.let { set(project.rootProject.file(it)) }
    }

    /**
     * Directory storing instance wide configuration files.
     */
    val configDir = sling.obj.dir {
        convention(projectDir.dir(sling.prop.string("instance.configPath") ?: "src/sling/instance"))
        sling.prop.file("instance.configDir")?.let { set(it) }
    }

    /**
     * Directory storing outputs of instance tasks.
     */
    val buildDir = sling.obj.dir {
        convention(projectDir.dir("build/instance"))
        sling.prop.file("instance.buildDir")?.let { set(it) }
    }

    val provisioner by lazy { Provisioner(this) }

    fun provisioner(options: Provisioner.() -> Unit) = provisioner.using(options)

    val tailer by lazy { Tailer(this) }

    fun tailer(options: Tailer.() -> Unit) = tailer.using(options)

    val statusReporter by lazy { StatusReporter(sling) }

    fun statusReporter(options: StatusReporter.() -> Unit) = statusReporter.using(options)

    // ===== Definition API =====

    /**
     * List of Sling instances e.g on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    val defined = sling.obj.list<Instance> {
        convention(sling.obj.provider {
            val fromCmd = sling.prop.string("instance.list")?.let {
                Instance.parse(sling, it) { env = Instance.ENV_CMD }
            } ?: listOf()
            val fromProperties = Instance.properties(sling)
            (fromCmd + fromProperties).ifEmpty { Instance.defaultPair(sling) }
        })
    }

    /**
     * Map of Sling instances with names as a keys.
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
        defined.add(sling.obj.provider { LocalInstance.create(sling, httpUrl, options) })
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
        defined.add(sling.obj.provider { Instance.create(sling, httpUrl, options) })
    }

    fun named(name: String) = defined.get().firstOrNull()
            ?: throw InstanceException("Instance named '$name' is not defined!")

    /**
     * Get defined instance by name or create temporary definition if URL provided.
     */
    fun parse(url: String): Instance = Instance.parse(sling, url).ifEmpty {
        throw InstanceException("Instance URL cannot be parsed properly '$url'!")
    }.single()

    // ===== Actions API =====

    fun awaitUp(instance: Instance, options: AwaitUpAction.() -> Unit = {}) = awaitUp(listOf(instance), options)

    fun awaitUp(instances: Collection<Instance> = sling.instances, options: AwaitUpAction.() -> Unit = {}) {
        AwaitUpAction(sling).apply(options).perform(instances)
    }

    fun awaitDown(instance: Instance, options: AwaitDownAction.() -> Unit = {}) = awaitDown(listOf(instance), options)

    fun awaitDown(instances: Collection<Instance> = sling.instances, options: AwaitDownAction.() -> Unit = {}) {
        AwaitDownAction(sling).apply(options).perform(instances)
    }

    fun awaitReloaded(instance: Instance, reloadOptions: ReloadAction.() -> Unit = {}, awaitUpOptions: AwaitUpAction.() -> Unit = {}) {
        awaitReloaded(listOf(instance), reloadOptions, awaitUpOptions)
    }

    fun awaitReloaded(
        instances: Collection<Instance> = sling.instances,
        reloadOptions: ReloadAction.() -> Unit = {},
        awaitUpOptions: AwaitUpAction.() -> Unit = {}
    ) {
        reload(instances, reloadOptions)
        awaitUp(instances, awaitUpOptions)
    }

    fun reload(instance: Instance, options: ReloadAction.() -> Unit = {}) = reload(listOf(instance), options)

    fun reload(instances: Collection<Instance> = sling.instances, options: ReloadAction.() -> Unit = {}) = ReloadAction(sling).apply(options).perform(instances)

    fun check(instance: Instance, options: CheckAction.() -> Unit) = check(listOf(instance), options)

    fun check(instances: Collection<Instance> = sling.instances, options: CheckAction.() -> Unit) = CheckAction(sling).apply(options).perform(instances)

    fun examine(instance: Instance) = examine(listOf(instance))

    /**
     * Checks as much as it can be despite type of instance before performing any other operations.
     *
     * Assumes that instances are already running.
     */
    fun examine(instances: Collection<Instance> = sling.instances) {
        examinePrerequisites(instances)
        examineAvailable(instances)
    }

    /**
     * Checks if local instances defined are meeting prerequisites before performing any other operations.
     *
     * Assumes that instances could not be running yet.
     */
    fun examinePrerequisites(instances: Collection<Instance> = sling.instances) {
        val localInstances = instances.filterIsInstance<LocalInstance>()
        if (localInstances.isNotEmpty()) {
            local.examinePrerequisites(localInstances)
        }
    }

    /**
     * Checks if instances are available before performing any other operations.
     */
    fun examineAvailable(instances: Collection<Instance> = sling.instances) {
        val unavailable = instances.filter { !it.available }
        if (unavailable.isNotEmpty()) {
            throw InstanceException("Some instances (${unavailable.size}) are unavailable:\n" +
                    unavailable.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}'" } + "\n\n" +
                    "Ensure having correct URLs defined, credentials correctly encoded and networking in correct state (internet accessible, VPN on/off)"
            )
        }
    }

    fun resolveFiles(instances: Collection<Instance> = sling.instances) {
        logger.info("Initializing resources needed by instance provisioner")
        provisioner.init(instances)
        logger.info("Initialized resources needed by instance provisioner")
    }
}
