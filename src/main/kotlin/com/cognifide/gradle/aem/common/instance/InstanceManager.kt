package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.CheckAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.aem.common.instance.provision.Provisioner
import com.cognifide.gradle.aem.common.instance.tail.Tailer
import com.cognifide.gradle.aem.instance.InstancePlugin
import com.cognifide.gradle.common.pluginProject
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.using

open class InstanceManager(val aem: AemExtension) {

    private val project = aem.project

    private val logger = project.logger

    val local by lazy { aem.localInstanceManager }

    val factory by lazy { InstanceFactory(aem) }

    /**
     * Using remote AEM instances is acceptable in any project, so that lookup for project applying local instance plugin is required
     * Needed to determine common directory storing instance related resources (tailer incident filter, Groovy scripts etc).
     */
    val projectDir = aem.obj.dir {
        convention(
            aem.obj.provider {
                project.pluginProject(InstancePlugin.ID)?.layout?.projectDirectory ?: throw InstanceException(
                    "Using remote AEM instances requires having at least one project applying plugin '${InstancePlugin.ID}'" +
                        " or setting property 'instance.projectDir'!"
                )
            }
        )
        aem.prop.string("instance.projectDir")?.let { set(project.rootProject.file(it)) }
    }

    /**
     * Directory storing instance wide configuration files.
     */
    val configDir = aem.obj.dir {
        convention(projectDir.dir(aem.prop.string("instance.configPath") ?: "src/aem/instance"))
        aem.prop.file("instance.configDir")?.let { set(it) }
    }

    /**
     * Directory storing outputs of instance tasks.
     */
    val buildDir = aem.obj.dir {
        convention(projectDir.dir("build/instance"))
        aem.prop.file("instance.buildDir")?.let { set(it) }
    }

    /**
     * Pattern to detect AEMaaCS instance
     */
    val cloudServicePattern = aem.obj.string {
        convention("*.adobeaemcloud.com")
        aem.prop.string("instance.cloudServicePattern")?.let { set(it) }
    }

    val provisioner by lazy { Provisioner(this) }

    fun provisioner(options: Provisioner.() -> Unit) = provisioner.using(options)

    val tailer by lazy { Tailer(this) }

    fun tailer(options: Tailer.() -> Unit) = tailer.using(options)

    val statusReporter by lazy { StatusReporter(aem) }

    fun statusReporter(options: StatusReporter.() -> Unit) = statusReporter.using(options)

    /**
     * Synchronize files with instances (interactively and with health checking).
     */
    val fileSync by lazy { FileSync(this) }

    fun fileSync(options: FileSync.() -> Unit) = FileSync(this).apply(options).sync()

    // ===== Definition API =====

    /**
     * List of AEM instances e.g on which packages could be deployed.
     * Instance stored in map ensures name uniqueness and allows to be referenced in expanded properties.
     */
    val defined = aem.obj.list<Instance> {
        convention(
            aem.obj.provider {
                val fromCmd = aem.prop.string("instance.list")?.let { httpUrls ->
                    (Formats.toList(httpUrls) ?: listOf()).map { factory.createByUrl(it) { cmd.set(true) } }
                } ?: listOf()
                val fromProperties = factory.parseProperties()
                (fromCmd + fromProperties).ifEmpty { factory.defaultPair() }.onEach { it.apply(definedOptions) }
            }
        )
    }

    private var definedOptions: Instance.() -> Unit = {}

    /**
     * Hook for additional configuration for defined instances.
     */
    fun defined(options: Instance.() -> Unit) {
        this.definedOptions = options
    }

    /**
     * Map of AEM instances with names as a keys.
     */
    val all = defined.map { p -> p.associateBy { it.name } }

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
    fun local(name: String, options: LocalInstance.() -> Unit = {}) {
        defined.add(aem.obj.provider { factory.local(name, options) })
    }

    /**
     * Define remote instance (available on any host).
     */
    fun remote(name: String, options: Instance.() -> Unit = {}) {
        defined.add(aem.obj.provider { factory.remote(name, options) })
    }

    fun find(name: String) = defined.get().firstOrNull { it.name == name }

    fun get(name: String) = find(name) ?: throw InstanceException("Instance named '$name' is not defined!")

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

    val examineEnabled = aem.obj.boolean {
        convention(aem.commonOptions.offline.map { !it })
        aem.prop.boolean("instance.examination.enabled")?.let { set(it) }
    }

    fun examine(instance: Instance) = examine(listOf(instance))

    /**
     * Checks as much as it can be despite type of instance before performing any other operations.
     *
     * Assumes that instances are already running.
     */
    fun examine(instances: Collection<Instance> = aem.instances) {
        if (!examineEnabled.get()) {
            return
        }

        examinePrerequisites(instances)
        examineAvailable(instances)
    }

    /**
     * Checks if local instances defined are meeting prerequisites before performing any other operations.
     *
     * Assumes that instances could not be running yet.
     */
    fun examinePrerequisites(instances: Collection<Instance> = aem.instances) {
        val localInstances = instances.filterIsInstance<LocalInstance>()
        if (localInstances.isNotEmpty()) {
            local.examinePrerequisites(localInstances)
        }
    }

    val examineAvailable = aem.obj.boolean {
        convention(examineEnabled)
        aem.prop.boolean("instance.examination.available")?.let { set(it) }
    }

    /**
     * Checks if instances are available before performing any other operations.
     */
    fun examineAvailable(instances: Collection<Instance> = aem.instances) {
        if (!examineAvailable.get()) {
            return
        }

        val cloudService = instances.filter { it.cloudService }
        if (cloudService.isNotEmpty()) {
            throw InstanceException(listOf(
                "Some instances (${cloudService.size}) are running as cloud services which makes it impossible to check their availability:",
                cloudService.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl.get()}'" },
                "",
                "AEM tasks use specific AEM endpoints that may be not available on AEMaaCS.",
                "The only way to use such instances is an implementation of custom tasks leveraging AEM DSL provided by the plugin.",
                "See: https://github.com/wttech/gradle-aem-plugin/blob/main/docs/common-plugin.md#implementing-tasks"
            ).joinToString("\n"))
        }

        val unavailable = instances.filter { !it.available }
        if (unavailable.isNotEmpty()) {
            throw InstanceException(listOf(
                "Some instances (${unavailable.size}) are unavailable:",
                unavailable.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl.get()}'" },
                "",
                "Ensure having correct URLs defined, credentials correctly encoded and networking in correct state (internet accessible, VPN on/off)"
            ).joinToString("\n"))
        }
    }

    fun resolveFiles(instances: Collection<Instance> = aem.instances) {
        logger.info("Initializing resources needed by instance provisioner")
        provisioner.init(instances)
        logger.info("Initialized resources needed by instance provisioner")
    }
}
