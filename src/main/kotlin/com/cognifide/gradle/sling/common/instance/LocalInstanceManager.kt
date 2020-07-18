package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingException
import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.action.AwaitDownAction
import com.cognifide.gradle.sling.common.instance.action.AwaitUpAction
import com.cognifide.gradle.sling.common.instance.local.*
import com.cognifide.gradle.sling.instance.LocalInstancePlugin
import com.cognifide.gradle.sling.javaVersions
import com.cognifide.gradle.common.pluginProject
import com.cognifide.gradle.common.utils.Patterns
import com.cognifide.gradle.common.utils.onEachApply
import com.cognifide.gradle.common.utils.using
import org.buildobjects.process.ProcBuilder
import org.gradle.api.JavaVersion
import java.io.File
import java.io.Serializable
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

class LocalInstanceManager(internal val sling: SlingExtension) : Serializable {

    private val project = sling.project

    private val common = sling.common

    private val logger = sling.logger

    val base by lazy { sling.instanceManager }

    /**
     * Using local Sling instances is acceptable in any project so that lookup for project applying local instance plugin is required
     * Needed to determine e.g directory in which Sling starter will be stored and override files.
     */
    val projectDir = sling.obj.dir {
        convention(sling.obj.provider {
            project.pluginProject(LocalInstancePlugin.ID)?.layout?.projectDirectory ?: throw LocalInstanceException(
                    "Using local Sling instances requires having at least one project applying plugin '${LocalInstancePlugin.ID}'" +
                    " or setting property 'localInstance.projectDir'!"
            )
        })
        sling.prop.string("localInstance.projectDir")?.let { set(project.rootProject.file(it)) }
    }

    /**
     * Path in which local Sling instances will be stored.
     */
    val rootDir = sling.obj.dir {
        convention(projectDir.dir(".gradle/sling/localInstance/instance"))
        sling.prop.file("localInstance.rootDir")?.let { set(it) }
    }

    /**
     * Path for storing local Sling instances related resources.
     */
    val configDir = sling.obj.dir {
        convention(projectDir.dir(sling.prop.string("localInstance.configPath") ?: "src/sling/localInstance"))
        sling.prop.file("localInstance.configDir")?.let { set(it) }
    }

    /**
     * Path from which e.g extra files for local Sling instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    val overrideDir = sling.obj.dir {
        convention(configDir.dir("override"))
        sling.prop.file("localInstance.overrideDir")?.let { set(it) }
    }

    /**
     * Determines how instances will be created (from backup or starter built from the scratch).
     */
    val source = sling.obj.typed<Source> {
        convention(Source.AUTO)
        sling.prop.string("localInstance.source")?.let { set(Source.of(it)) }
    }

    fun source(name: String) {
        source.set(Source.of(name))
    }

    /**
     * Automatically open a web browser when instances are up.
     */
    val openMode = sling.obj.typed<OpenMode> {
        convention(OpenMode.NEVER)
        sling.prop.string("localInstance.openMode")?.let { set(OpenMode.of(it)) }
    }

    /**
     * Maximum time to wait for browser open command response.
     */
    val openTimeout = sling.obj.long {
        convention(TimeUnit.SECONDS.toMillis(30))
        sling.prop.long("localInstance.openTimeout")?.let { set(it) }
    }

    fun openMode(name: String) {
        openMode.set(OpenMode.of(name))
    }

    fun resolveFiles() {
        logger.info("Resolving local instance files")
        logger.info("Resolved local instance files:\n${sourceFiles.joinToString("\n")}")
    }

    /**
     * Maximum time to wait for status script response.
     */
    val statusTimeout = sling.obj.long {
        convention(TimeUnit.SECONDS.toMillis(5))
        sling.prop.long("localInstance.statusTimeout")?.let { set(it) }
    }

    /**
     * Collection of files potentially needed to create instance
     */
    val sourceFiles = sling.obj.files {
        from(sling.obj.provider {
            listOfNotNull(backupZip) + starter.files + install.files
        })
    }

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    val expandFiles = sling.obj.strings {
        convention(listOf(
                "**/start.bat",
                "**/stop.bat",
                "**/start",
                "**/stop",
                "**/status.bat",
                "**/status"
        ))
    }

    /**
     * Custom properties that can be injected into instance files.
     */
    val expandProperties = sling.obj.map<String, Any> { convention(mapOf()) }

    val starter by lazy { StarterResolver(sling) }

    /**
     * Configure Sling source files when creating instances from the scratch.
     */
    fun starter(options: StarterResolver.() -> Unit) = starter.using(options)

    /**
     * Configure Sling backup sources.
     */
    val backup by lazy { BackupManager(sling) }

    fun backup(options: BackupManager.() -> Unit) = backup.using(options)

    val backupZip: File?
        get() {
            return when (source.get()) {
                Source.AUTO, Source.BACKUP_ANY -> backup.any
                Source.BACKUP_LOCAL -> backup.local
                Source.BACKUP_REMOTE -> backup.remote
                else -> null
            }
        }

    val install by lazy { InstallResolver(sling) }

    /**
     * Configure CRX packages, bundles to be pre-installed on instance(s).
     */
    fun install(options: InstallResolver.() -> Unit) = install.using(options)

    internal var initOptions: LocalInstance.() -> Unit = {}

    /**
     * Configure action to be performed only once when instance is up first time.
     */
    fun init(options: LocalInstance.() -> Unit) {
        this.initOptions = options
    }

    fun create(instance: LocalInstance) = create(listOf(instance))

    fun create(instances: Collection<LocalInstance> = sling.localInstances): List<LocalInstance> {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.lifecycle("No instances to create.")
            return listOf()
        }

        logger.info("Creating instances: ${uncreatedInstances.names}")
        createBySource(uncreatedInstances)

        return uncreatedInstances.filter { it.created }
    }

    @Suppress("ComplexMethod")
    fun createBySource(instances: Collection<LocalInstance> = sling.localInstances) = when (source.get()) {
        Source.AUTO -> {
            val backupZip = backup.any
            when {
                backupZip != null -> createFromBackup(instances, backupZip)
                else -> createFromScratch(instances)
            }
        }
        Source.BACKUP_ANY -> {
            val backupZip = backup.any
            when {
                backupZip != null -> createFromBackup(instances, backupZip)
                else -> throw LocalInstanceException("Cannot create instance(s) because no backups available!")
            }
        }
        Source.BACKUP_LOCAL -> {
            val backupZip = backup.local
                    ?: throw LocalInstanceException("Cannot create instance(s) because no local backups available!")
            createFromBackup(instances, backupZip)
        }
        Source.BACKUP_REMOTE -> {
            val backupZip = backup.remote
                    ?: throw LocalInstanceException("Cannot create instance(s) because no remote backups available!")
            createFromBackup(instances, backupZip)
        }
        Source.SCRATCH -> createFromScratch(instances)
        null -> {}
    }

    fun createFromBackup(instances: Collection<LocalInstance> = sling.localInstances, backupZip: File) {
        backup.restore(backupZip, rootDir.get().asFile, instances)

        val missingInstances = instances.filter { !it.created }
        if (missingInstances.isNotEmpty()) {
            logger.info("Backup ZIP '$backupZip' does not contain all instances. Creating from scratch: ${missingInstances.names}")

            createFromScratch(missingInstances)
        }

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Customizing instance '$name'") {
                    logger.info("Customizing: $this")
                    customize()
                    logger.info("Customized: $this")
                }
            }
        }
    }

    fun createFromScratch(instances: Collection<LocalInstance> = sling.localInstances) {
        if (starter.jar == null) {
            throw LocalInstanceException("Cannot create instances due to lacking source files. " +
                    "Ensure having specified local instance starter JAR url.")
        }

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Creating instance '$name'") {
                    if (created) {
                        logger.info(("Instance already created: $this"))
                        return@increment
                    }

                    logger.info("Creating: $this")
                    prepare()
                    logger.info("Created: $this")
                }
            }
        }
    }

    fun destroy(instance: LocalInstance): Boolean = destroy(listOf(instance)).isNotEmpty()

    fun destroy(instances: Collection<LocalInstance> = sling.localInstances): List<LocalInstance> {
        val createdInstances = instances.filter { it.touched }
        if (createdInstances.isEmpty()) {
            logger.lifecycle("No instances to destroy.")
            return listOf()
        }

        logger.info("Destroying instance(s): ${createdInstances.names}")

        common.progress(createdInstances.size) {
            createdInstances.onEachApply {
                increment("Destroying '$name'") {
                    logger.info("Destroying: $this")
                    delete()
                    logger.info("Destroyed: $this")
                }
            }
        }

        return createdInstances
    }

    fun up(instance: LocalInstance, awaitUpOptions: AwaitUpAction.() -> Unit = {}) = up(listOf(instance), awaitUpOptions).isNotEmpty()

    fun up(instances: Collection<LocalInstance> = sling.localInstances, awaitUpOptions: AwaitUpAction.() -> Unit = {}): List<LocalInstance> {
        val downInstances = instances.filter { !it.running }
        if (downInstances.isEmpty()) {
            logger.lifecycle("No instances to turn on.")
            return listOf()
        }

        common.progress(downInstances.size) {
            downInstances.onEachApply {
                increment("Customizing instance '$name'") {
                    logger.info("Customizing: $this")
                    customize()
                    logger.info("Customized: $this")
                }
            }
        }

        common.progress(downInstances.size) {
            common.parallel.with(downInstances) {
                increment("Starting instance '$name'") {
                    if (!created) {
                        logger.info("Instance not created, so it could not be up: $this")
                        return@increment
                    }

                    val status = checkStatus()
                    if (status == Status.RUNNING) {
                        logger.info("Instance already running. No need to start: $this")
                        return@increment
                    }

                    executeStartScript()
                }
            }
        }

        base.awaitUp(downInstances, awaitUpOptions)

        val uninitializedInstances = downInstances.filter { !it.initialized }
        common.progress(uninitializedInstances.size) {
            common.parallel.with(uninitializedInstances) {
                increment("Initializing instance '$name'") {
                    logger.info("Initializing: $this")
                    init(initOptions)
                }
            }
        }

        when {
            openMode.get() == OpenMode.ALWAYS -> open(downInstances)
            openMode.get() == OpenMode.ONCE -> open(uninitializedInstances)
        }

        return downInstances
    }

    fun down(instance: LocalInstance, awaitDownOptions: AwaitDownAction.() -> Unit = {}) = down(listOf(instance), awaitDownOptions).isNotEmpty()

    fun down(instances: Collection<LocalInstance> = sling.localInstances, awaitDownOptions: AwaitDownAction.() -> Unit = {}): List<LocalInstance> {
        val upInstances = instances.filter { it.running }
        if (upInstances.isEmpty()) {
            logger.lifecycle("No instances to turn off.")
            return listOf()
        }

        common.progress(upInstances.size) {
            common.parallel.with(upInstances) {
                increment("Stopping instance '$name'") {
                    if (!created) {
                        logger.info("Instance not created, so it could not be down: $this")
                        return@increment
                    }

                    val status = checkStatus()
                    if (status != Status.RUNNING) {
                        logger.info("Instance is not running (reports status '$status'). No need to stop: $this")
                        return@increment
                    }

                    executeStopScript()
                }
            }
        }

        base.awaitDown(upInstances, awaitDownOptions)

        return upInstances
    }

    fun open(instance: LocalInstance) = open(listOf(instance))

    fun open(instances: Collection<LocalInstance> = sling.localInstances): List<LocalInstance> {
        val upInstances = instances.filter { it.running }
        if (upInstances.isEmpty()) {
            logger.lifecycle("No instances to open.")
            return listOf()
        }

        val openedInstances = CopyOnWriteArrayList<LocalInstance>()
        common.progress(upInstances.size) {
            common.parallel.with(upInstances) {
                increment("Opening instance '$name'") {
                    try {
                        sling.webBrowser.open(httpOpenUrl) { withTimeoutMillis(openTimeout.get()) }
                        openedInstances += this@with
                    } catch (e: SlingException) {
                        logger.debug("Instance '$name' open error", e)
                        logger.warn("Cannot open instance '$name'! Cause: ${e.message}")
                    }
                }
            }
        }
        if (openedInstances.isNotEmpty()) {
            logger.lifecycle("Opened instances (${openedInstances.size}) in web browser (tabs):\n" +
                    openedInstances.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpOpenUrl}'" })
        }

        return openedInstances
    }

    fun kill(instance: LocalInstance) = kill(listOf(instance))

    fun kill(instances: Collection<LocalInstance> = sling.localInstances): List<LocalInstance> {
        val killableInstances = instances.filter { it.pid > 0 }
        if (killableInstances.isEmpty()) {
            logger.lifecycle("No instances to kill.")
            return listOf()
        }

        val killedInstances = CopyOnWriteArrayList<LocalInstance>()
        common.progress(killableInstances.size) {
            common.parallel.with(killableInstances) {
                increment("Killing instance '$name'") {
                    try {
                        sling.processKiller.kill(pid)
                        killedInstances += this@with
                    } catch (e: SlingException) {
                        logger.debug("Instance '$name' kill error", e)
                        logger.warn("Cannot kill instance '$name'! Cause: ${e.message}")
                    }
                }
            }
        }

        if (killedInstances.isEmpty()) {
            logger.lifecycle("No instances killed!")
        }

        return killedInstances
    }

    fun examine(instances: Collection<LocalInstance> = sling.localInstances) = base.examine(instances)

    fun examinePrerequisites(instances: Collection<LocalInstance> = sling.localInstances) {
        examinePaths()
        examineJavaAvailable()
        examineRunningOther(instances)
    }

    fun examinePaths() {
        val rootDir = rootDir.get().asFile
        if (rootDir.path.contains(" ")) {
            throw LocalInstanceException("Local instances root path must not contain spaces: '$rootDir'!\n" +
                    "Sling control scripts could run improperly with such paths.")
        }
    }

    /**
     * Pre-conditionally check if 'java' is available in shell scripts.
     *
     * Gradle is intelligently looking by its own for installed Java, but Sling control scripts
     * are just requiring 'java' command available in 'PATH' environment variable.
     */
    @Suppress("TooGenericExceptionCaught", "MagicNumber")
    fun examineJavaAvailable() {
        try {
            logger.debug("Examining Java properly installed")

            val result = ProcBuilder("java", "-version")
                    .withWorkingDirectory(rootDir.get().asFile.apply { mkdirs() })
                    .withTimeoutMillis(statusTimeout.get())
                    .withExpectedExitStatuses(0)
                    .run()
            logger.debug("Installed Java:\n${result.outputString.ifBlank { result.errorString }}")
        } catch (e: Exception) {
            throw LocalInstanceException("Local instances support requires Java properly installed! Cause: '${e.message}'\n" +
                    "Ensure having directory with 'java' executable listed in 'PATH' environment variable.", e)
        }
    }

    fun examineRunningOther(instances: Collection<LocalInstance> = sling.localInstances) {
        val running = instances.filter { it.runningOther }
        if (running.isNotEmpty()) {
            throw LocalInstanceException("Other instances (${running.size}) are running:\n" +
                    running.joinToString("\n") { "Instance '${it.name}' at URL '${it.httpUrl}' located at path '${it.runningDir}'" } + "\n\n" +
                    "Ensure having these instances down."
            )
        }
    }
}
