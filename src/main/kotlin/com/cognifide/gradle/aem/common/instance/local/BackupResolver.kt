package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.instance.tasks.InstanceBackup
import java.io.File

class BackupResolver(private val aem: AemExtension) {

    val uploadUrl = aem.props.string("localInstance.backup.uploadUrl")

    val downloadUrl = aem.props.string("localInstance.backup.downloadUrl")

    /**
     * Defines backup selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup
     * (file names sorted lexically / descending).
     */
    var selector: Collection<String>.() -> String? = {
        val backupName = aem.props.string("localInstance.backup.name") ?: ""
        when {
            backupName.isNotBlank() -> firstOrNull { it == backupName }
            else -> sortedByDescending { it }.firstOrNull()
        }
    }

    val internal: File
        get() = resolve(internalSources)

    val external: File
        get() = resolve(externalSources)

    val auto: File = resolve(internalSources + externalSources)

    private val internalSources: List<BackupSource>
        get() = aem.tasks.named<InstanceBackup>(InstanceBackup.NAME).get().available.map { BackupSource(it.name) { it } }

    private val externalSources: List<BackupSource>
        get() = when {
            downloadUrl != null -> {
                val dirUrl = downloadUrl.substringBeforeLast("/")
                val name = downloadUrl.substringAfterLast("/")

                listOf(BackupSource(name) {
                    val file = aem.temporaryFile(name)
                    aem.fileTransfer.download(dirUrl, name, file)
                    file
                })
            }
            uploadUrl != null -> {
                val names = aem.fileTransfer.list(uploadUrl)

                names.map { name ->
                    BackupSource(name) {
                        val file = aem.temporaryFile(name) // TODO improve it
                        aem.fileTransfer.download(uploadUrl, name, file)
                        file
                    }
                }
            }
            else -> listOf()
        }

    private fun resolve(sources: List<BackupSource>): File {
        if (sources.isEmpty()) {
            throw InstanceException("No backups available")
        }

        val backups = sources.map { it.name }
        val backup = selector(backups)
        val source = sources.firstOrNull { it.name == backup }

        return source?.file ?: throw InstanceException("No backup selected")
    }
}