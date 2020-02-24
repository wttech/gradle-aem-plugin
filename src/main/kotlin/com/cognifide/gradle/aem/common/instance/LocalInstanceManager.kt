package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.local.*
import com.cognifide.gradle.aem.common.utils.onEachApply
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable
import java.util.concurrent.TimeUnit

class LocalInstanceManager(private val aem: AemExtension) : Serializable {

    private val common = aem.common

    private val logger = aem.logger

    private val base = aem.instanceManager

    /**
     * Path from which e.g extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    val configDir = aem.obj.dir {
        convention(base.configDir.dir("local"))
        aem.prop.file("localInstance.configDir")?.let { set(it) }
    }

    /**
     * Path in which local AEM instances will be stored.
     */
    val rootDir = aem.obj.dir {
        convention(aem.obj.projectDir(".instance"))
        aem.prop.file("localInstance.root")?.let { set(it) }
    }

    /**
     * Determines how instances will be created (from backup or quickstart built from the scratch).
     */
    val source = aem.obj.typed<Source> {
        convention(Source.AUTO)
        aem.prop.string("localInstance.source")?.let { set(Source.of(it)) }
    }

    fun source(name: String) {
        source.set(Source.of(name))
    }

    fun resolveSourceFiles() {
        logger.info("Resolving local instance files")
        logger.info("Resolved local instance files:\n${sourceFiles.joinToString("\n")}")
    }

    /**
     * Maximum time to wait for status script response.
     */
    val scriptTimeout = aem.obj.long {
        convention(TimeUnit.SECONDS.toMillis(5))
        aem.prop.long("localInstance.scriptTimeout")?.let { set(it) }
    }

    /**
     * Collection of files potentially needed to create instance
     */
    @get:JsonIgnore
    val sourceFiles = aem.obj.files {
        from(aem.obj.provider {
            listOfNotNull(backupZip) + quickstart.files + install.files
        })
    }

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    val expandFiles = aem.obj.strings {
        convention(listOf(
                "**/start.bat",
                "**/stop.bat",
                "**/start",
                "**/stop"
        ))
    }

    /**
     * Custom properties that can be injected into instance files.
     */
    val expandProperties = aem.obj.map<String, Any> {
        convention(mapOf())
    }

    val quickstart = QuickstartResolver(aem)

    /**
     * Configure AEM source files when creating instances from the scratch.
     */
    fun quickstart(options: QuickstartResolver.() -> Unit) {
        quickstart.apply(options)
    }

    /**
     * Configure AEM backup sources.
     */
    val backup = BackupResolver(aem)

    fun backup(options: BackupResolver.() -> Unit) {
        backup.apply(options)
    }

    @get:JsonIgnore
    val backupZip: File?
        get() {
            return when (source.get()) {
                Source.AUTO, Source.BACKUP_ANY -> backup.any
                Source.BACKUP_LOCAL -> backup.local
                Source.BACKUP_REMOTE -> backup.remote
                else -> null
            }
        }

    val install = InstallResolver(aem)

    /**
     * Configure CRX packages, bundles to be pre-installed on instance(s).
     */
    fun install(options: InstallResolver.() -> Unit) {
        install.apply(options)
    }

    @get:JsonIgnore
    internal var initOptions: LocalInstance.() -> Unit = {}

    /**
     * Configure action to be performed only once when instance is up first time.
     */
    fun init(options: LocalInstance.() -> Unit) {
        this.initOptions = options
    }

    fun create(instance: LocalInstance) = create(listOf(instance))

    fun create(instances: Collection<LocalInstance>): List<LocalInstance> {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to create")
            return listOf()
        }

        logger.info("Creating instances: ${uncreatedInstances.names}")
        createBySource(uncreatedInstances)

        return uncreatedInstances.filter { it.created }
    }

    @Suppress("ComplexMethod")
    fun createBySource(instances: Collection<LocalInstance>) {
        when (source.get()) {
            Source.AUTO -> {
                val backupZip = backup.any
                if (backupZip != null) {
                    createFromBackup(instances, backupZip)
                } else {
                    createFromScratch(instances)
                }
            }
            Source.BACKUP_ANY -> {
                val backupZip = backup.any
                if (backupZip != null) {
                    createFromBackup(instances, backupZip)
                } else {
                    throw InstanceException("Cannot create instance(s) because no backups available!")
                }
            }
            Source.BACKUP_LOCAL -> {
                val backupZip = backup.local
                        ?: throw InstanceException("Cannot create instance(s) because no local backups available!")
                createFromBackup(instances, backupZip)
            }
            Source.BACKUP_REMOTE -> {
                val backupZip = backup.remote
                        ?: throw InstanceException("Cannot create instance(s) because no remote backups available!")
                createFromBackup(instances, backupZip)
            }
            Source.SCRATCH -> createFromScratch(instances)
            null -> {}
        }
    }

    fun createFromBackup(instances: Collection<LocalInstance>, backupZip: File) {
        logger.info("Restoring instances from backup ZIP '$backupZip' to directory '$rootDir'")

        rootDir.get().asFile.mkdirs()

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Restoring instance '$name'") {
                    FileOperations.zipUnpackDir(backupZip, id, rootDir.get().asFile)
                }
            }
        }

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

    fun createFromScratch(instances: Collection<LocalInstance>) {
        if (quickstart.jar == null || quickstart.license == null) {
            throw InstanceException("Cannot create instances due to lacking source files. " +
                    "Ensure having specified local instance quickstart jar & license urls.")
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

    fun destroy(instances: Collection<LocalInstance>): List<LocalInstance> {
        val createdInstances = instances.filter { it.touched }
        if (createdInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to destroy")
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

    fun up(instances: Collection<LocalInstance>, awaitUpOptions: AwaitUpAction.() -> Unit = {}): List<LocalInstance> {
        val downInstances = instances.filter { !it.running }
        if (downInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to turn on")
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

        common.progress(downInstances.size) {
            common.parallel.with(downInstances) {
                increment("Initializing instance '$name'") {
                    saveVersion()
                    if (!initialized) {
                        logger.info("Initializing: $this")
                        init(initOptions)
                    }
                }
            }
        }

        return downInstances
    }

    fun down(instance: LocalInstance, awaitDownOptions: AwaitDownAction.() -> Unit = {}) = down(listOf(instance), awaitDownOptions).isNotEmpty()

    fun down(instances: Collection<LocalInstance>, awaitDownOptions: AwaitDownAction.() -> Unit = {}): List<LocalInstance> {
        val upInstances = instances.filter { it.running }
        if (upInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to turn off")
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
}
