package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.local.BackupResolver
import com.cognifide.gradle.aem.common.instance.local.QuickstartResolver
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.common.utils.onEachApply
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable

class LocalInstanceManager(private val aem: AemExtension) : Serializable {

    /**
     * Path in which local AEM instances will be stored.
     */
    var rootDir: File = aem.props.string("localInstance.root")?.let { aem.project.file(it) }
            ?: aem.projectMain.file(".aem/instance")

    /**
     * Determines how instances will be created (from backup or quickstart built from the scratch).
     */
    var source = LocalInstanceSource.of(aem.props.string("localInstance.source")
            ?: LocalInstanceSource.AUTO.name)

    /**
     * Path from which extra files for local AEM instances will be copied.
     * Useful for overriding default startup scripts ('start.bat' or 'start.sh') or providing some files inside 'crx-quickstart'.
     */
    var overridesDir = File(aem.configCommonDir, LocalInstance.FILES_PATH)

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

    fun quickstart(options: QuickstartResolver.() -> Unit) {
        quickstart.apply(options)
    }

    val backup = BackupResolver(aem)

    fun backup(options: BackupResolver.() -> Unit) {
        backup.apply(options)
    }

    @get:JsonIgnore
    val backupZip: File?
        get() {
            return when (source) {
                LocalInstanceSource.AUTO -> backup.auto
                LocalInstanceSource.BACKUP_INTERNAL -> backup.internal
                LocalInstanceSource.BACKUP_EXTERNAL -> backup.external
                LocalInstanceSource.SCRATCH -> null
            }
        }

    fun create(instances: List<LocalInstance>) {
        val backupZip = backupZip
        if (backupZip != null) {
            createFromBackup(instances, backupZip)
        } else {
            createFromScratch(instances)
        }
    }

    fun createFromBackup(instances: List<LocalInstance>, backupZip: File) {
        aem.logger.info("Extracting files from backup ZIP '$backupZip' to directory '$rootDir'")

        val backupSize = Formats.size(backupZip)

        aem.progressIndicator {
            message = "Extracting backup ZIP: ${backupZip.name}, size: $backupSize"

            if (rootDir.exists()) {
                rootDir.deleteRecursively()
            }
            rootDir.mkdirs()
            FileOperations.zipUnpack(backupZip, rootDir)
        }

        val missingInstances = instances.filter { !it.created }
        if (missingInstances.isNotEmpty()) {
            aem.logger.info("Backup ZIP '$backupZip' does not contain all instances. Creating from scratch: ${missingInstances.names}")

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