package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.LocalInstanceOptions
import com.cognifide.gradle.aem.instance.names
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Create : Instance() {

    @Internal
    val options = LocalInstanceOptions(aem, AemTask.temporaryDir(project, name))

    init {
        description = "Creates local AEM instance(s)."
    }

    fun options(options: LocalInstanceOptions.() -> Unit) {
        this.options.apply(options)
    }

    @TaskAction
    fun create() {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.info("No instance(s) to create")
            return
        }

        logger.info("Creating instances: ${uncreatedInstances.names}")

        val backupZip = when (options.source) {
            LocalInstanceOptions.Source.AUTO -> findRecentBackup()
            LocalInstanceOptions.Source.BACKUP_INTERNAL -> getInternalBackup()
            LocalInstanceOptions.Source.BACKUP_EXTERNAL -> getExternalBackup()
            LocalInstanceOptions.Source.NONE -> null
        }
        if (backupZip != null) {
            val instanceRoot = File(aem.config.instanceRoot)

            aem.logger.info("Extracting files from backup ZIP '$backupZip' to directory '$instanceRoot'")
            aem.progressIndicator {
                message = "Extracting backup ZIP '${backupZip.name}'"
                FileOperations.zipUnpackSafe(backupZip, instanceRoot)
            }
        } else {
            if (options.jar == null || options.license == null) {
                throw InstanceException("Cannot create instances due to lacking source files. " +
                        "Ensure having specified: local instance ZIP url or jar & license url.")
            }

            aem.progress(uncreatedInstances.size) {
                uncreatedInstances.onEachApply {
                    increment("Instance '$name'") {
                        create(options)
                    }
                }
            }
        }

        aem.notifier.notify("Instance(s) created", "Which: ${uncreatedInstances.names}")
    }

    private fun findRecentBackup() = findRecentBackup(options.zip)

    private fun findRecentBackup(externalZip: File?): File? {
        val external = if (externalZip == null) listOf() else listOf(externalZip)
        val internal = aem.tasks.named<Backup>(Backup.NAME).get().available

        return options.zipSelector(external + internal)
    }

    private fun getInternalBackup(): File? {
        return findRecentBackup(null) ?: throw InstanceException("Internal local instance backup is not yet created. " +
                "Ensure running task 'aemBackup' before.")
    }

    private fun getExternalBackup(): File {
        return options.zip ?: throw InstanceException("External local instance backup is not available. " +
                "Ensure having property 'aem.localInstance.zipUrl' specified.")
    }

    companion object {
        const val NAME = "aemCreate"
    }
}