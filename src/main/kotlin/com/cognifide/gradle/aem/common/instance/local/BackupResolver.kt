package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.instance.tasks.InstanceBackup
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class BackupResolver(private val aem: AemExtension) {

    var uploadUrl = aem.props.string("localInstance.backup.uploadUrl")

    var downloadUrl = aem.props.string("localInstance.backup.downloadUrl")

    var downloadDir: File = aem.temporaryDir("backup/external")

    /**
     * Defines backup source selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup
     * (file names sorted lexically / descending).
     */
    @get:JsonIgnore
    var selector: Collection<BackupSource>.() -> BackupSource? = {
        val backupName = aem.props.string("localInstance.backup.name") ?: ""
        when {
            backupName.isNotBlank() -> firstOrNull { it.fileName == backupName }
            else -> sortedByDescending { it.fileName }.firstOrNull()
        }
    }

    @get:JsonIgnore
    val internal: File
        get() = resolve(internalSources)

    @get:JsonIgnore
    val external: File
        get() = resolve(externalSources)

    @get:JsonIgnore
    val auto: File
        get() = resolve(internalSources + externalSources)

    private val internalSources: List<BackupSource>
        get() = aem.tasks.named<InstanceBackup>(InstanceBackup.NAME).get().available.map {
            BackupSource(BackupType.INTERNAL, it.name) { it }
        }

    private val externalSources: List<BackupSource>
        get() = when {
            downloadUrl != null -> {
                val dirUrl = downloadUrl!!.substringBeforeLast("/")
                val name = downloadUrl!!.substringAfterLast("/")

                listOf(
                    BackupSource(BackupType.EXTERNAL, name) {
                        File(downloadDir, name).apply {
                            aem.fileTransfer.download(dirUrl, name, this)
                        }
                    }
                )
            }
            uploadUrl != null -> {
                aem.fileTransfer.list(uploadUrl!!).map { name ->
                    BackupSource(BackupType.EXTERNAL, name) {
                        File(downloadDir, name).apply {
                            aem.fileTransfer.download(uploadUrl!!, name, this)
                        }
                    }
                }
            }
            else -> listOf()
        }

    private fun resolve(sources: List<BackupSource>): File {
        if (sources.isEmpty()) {
            throw InstanceException("No backups available")
        }

        val source = selector(sources)

        return source?.file ?: throw InstanceException("No backup selected")
    }
}