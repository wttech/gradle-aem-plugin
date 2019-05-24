package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.LocalInstance
import com.cognifide.gradle.aem.instance.names
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class InstanceRestore : LocalInstanceTask() {

    private val downloadDir = AemTask.temporaryDir(aem.project, TEMPORARY_DIR)

    private val fileResolver = FileResolver(aem, downloadDir).apply { group(GROUP_EXTRA) {} }

    private val localBackups: List<File>
        get() {
            return aem.tasks.named<InstanceBackup>(InstanceBackup.NAME).get().available
        }

    @Internal
    val uploadUrl = aem.props.string("backup.uploadUrl")

    @Internal
    val backupUrl = aem.props.string("backup.downloadUrl")

    init {
        description = "Restores AEM instance(s) from backup file."
    }

    /**
     * Defines backup selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup
     * (file names sorted lexically / descending).
     */
    @JsonIgnore
    var backupSelector: Collection<String>.() -> String? = {
        val backupName = aem.props.string("backup.name") ?: ""
        when {
            backupName.isNotBlank() -> firstOrNull { it == backupName }
            else -> sortedByDescending { it }.firstOrNull()
        }
    }

    @TaskAction
    fun restore() {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.info("No instance(s) to restore")
            return
        }

        logger.info("Restoring instances: ${uncreatedInstances.names}")
        restoreFromBackup(uncreatedInstances, selectBackup())

        val restoredInstances = uncreatedInstances.filter { it.created }
        aem.notifier.notify("Instance(s) restored", "Which: ${restoredInstances.names}")

        aem.tasks.named<InstanceUp>(InstanceUp.NAME).get().up()
    }

    private fun selectBackup() = when {
        backupUrl != null -> fileResolver.run { backupUrl.run { url(this) } }.file
        uploadUrl != null -> {
            val name = backupSelector(aem.fileTransfer.list(uploadUrl))
                    ?: throw AemException("No backups to restore. Please perform backup before restoring.")
            fileResolver.run { backupUrl(uploadUrl, name).run { url(this) } }.file
        }
        localBackups.isNotEmpty() -> {
            val name = backupSelector(localBackups.map { it.name })
            localBackups.find { it.name == name }
                    ?: throw AemException("No local backups to restore. Please perform backup before restoring.")
        }
        else -> throw AemException("No backups to restore. Please specify one of properties: " +
                "['backup.downloadUrl', 'backup.uploadUrl'] or perform backup locally before restoring.")
    }

    private fun restoreFromBackup(uncreatedInstances: List<LocalInstance>, backupZip: File) {
        if (instances != uncreatedInstances) {
            throw InstanceException("Backup ZIP cannot be used to create missing instances.")
        }

        val instanceRoot = aem.config.localInstanceOptions.rootDir

        aem.logger.info("Extracting files from backup ZIP '$backupZip' to directory '$instanceRoot'")

        val backupSize = Formats.size(backupZip)

        aem.progressIndicator {
            message = "Extracting backup ZIP: ${backupZip.name}, size: $backupSize"

            if (instanceRoot.exists()) {
                instanceRoot.deleteRecursively()
            }
            instanceRoot.mkdirs()
            FileOperations.zipUnpack(backupZip, instanceRoot)
        }

        aem.progress(uncreatedInstances.size) {
            uncreatedInstances.onEachApply {
                increment("Customizing instance '$name'") {
                    customize()
                }
            }
        }
    }

    private fun backupUrl(uploadUrl: String, name: String) = if (uploadUrl.endsWith("/")) {
        "$uploadUrl$name"
    } else {
        "$uploadUrl/$name"
    }

    companion object {
        const val NAME = "instanceRestore"
        const val TEMPORARY_DIR = "backup"
        const val GROUP_EXTRA = "extra"
    }
}