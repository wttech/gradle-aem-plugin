package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.local.BackupResolver
import com.cognifide.gradle.aem.common.instance.local.InstallResolver
import com.cognifide.gradle.aem.common.instance.local.QuickstartResolver
import com.cognifide.gradle.aem.common.instance.local.Source
import com.cognifide.gradle.aem.common.utils.onEachApply
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable
import java.util.concurrent.TimeUnit

class LocalInstanceManager(private val aem: AemExtension) : Serializable {

    private val logger = aem.logger

    /**
     * Path in which local AEM instances will be stored.
     */
    var rootDir: File = aem.prop.string("localInstance.root")?.let { aem.project.file(it) }
            ?: aem.projectMain.file(".instance")

    /**
     * Determines how instances will be created (from backup or quickstart built from the scratch).
     */
    var source = Source.of(aem.prop.string("localInstance.source") ?: Source.AUTO.name)

    fun source(name: String) {
        source = Source.of(name)
    }

    fun resolveSourceFiles() {
        logger.info("Resolving local instance files")
        logger.info("Resolved local instance files:\n${sourceFiles.joinToString("\n")}")
    }

    /**
     * Maximum time to wait for status script response.
     */
    var scriptTimeout: Long = aem.prop.long("localInstance.scriptTimeout") ?: TimeUnit.SECONDS.toMillis(5)

    /**
     * Collection of files potentially needed to create instance
     */
    @get:JsonIgnore
    val sourceFiles: List<File>
        get() = listOfNotNull(backupZip) + quickstart.files + install.files

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    var overridesDir = aem.configCommonDir.resolve(LocalInstance.FILES_PATH)

    /**
     * Wildcard file name filter expression that is used to filter in which instance files properties can be injected.
     */
    var expandFiles: List<String> = listOf(
            "**/start.bat",
            "**/stop.bat",
            "**/start",
            "**/stop"
    )

    /**
     * Custom properties that can be injected into instance files.
     */
    var expandProperties: Map<String, Any> = mapOf()

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
            return when (source) {
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

    @Suppress("ComplexMethod")
    fun create(instances: List<LocalInstance>) {
        when (source) {
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
        }
    }

    fun createFromBackup(instances: List<LocalInstance>, backupZip: File) {
        logger.info("Restoring instances from backup ZIP '$backupZip' to directory '$rootDir'")

        rootDir.mkdirs()

        aem.progress(instances.size) {
            instances.onEachApply {
                increment("Restoring instance '$name'") {
                    FileOperations.zipUnpackDir(backupZip, id, rootDir)
                }
            }
        }

        val missingInstances = instances.filter { !it.created }
        if (missingInstances.isNotEmpty()) {
            logger.info("Backup ZIP '$backupZip' does not contain all instances. Creating from scratch: ${missingInstances.names}")

            createFromScratch(missingInstances)
        }

        aem.progress(instances.size) {
            instances.onEachApply {
                increment("Customizing instance '$name'") {
                    customize()
                }
            }
        }
    }

    fun createFromScratch(instances: List<LocalInstance>) {
        if (quickstart.jar == null || quickstart.license == null) {
            throw InstanceException("Cannot create instances due to lacking source files. " +
                    "Ensure having specified local instance quickstart jar & license urls.")
        }

        aem.progress(instances.size) {
            instances.onEachApply {
                increment("Creating instance '$name'") {
                    create()
                }
            }
        }
    }
}
