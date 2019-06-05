package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.instance.tasks.InstanceBackup
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class BackupResolver(private val aem: AemExtension) {

    var uploadUrl = aem.props.string("localInstance.backup.uploadUrl")

    var downloadUrl = aem.props.string("localInstance.backup.downloadUrl")

    var downloadDir: File = aem.temporaryDir("backup/remote")

    /**
     * Defines backup source selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup.
     * File names sorted lexically / descending. If same name on local & remote source found, local has precedence.
     */
    @get:JsonIgnore
    var selector: Collection<BackupSource>.() -> BackupSource? = {
        val sorted = sortedWith(compareByDescending<BackupSource> { it.fileEntry.name }.thenBy { it.type.ordinal })
        val name = aem.props.string("localInstance.backup.name") ?: ""

        when {
            name.isNotBlank() -> sorted.firstOrNull { it.fileEntry.name == name }
            else -> sorted.firstOrNull()
        }
    }

    @get:JsonIgnore
    val local: File?
        get() = resolve(localSources)

    @get:JsonIgnore
    val remote: File?
        get() = resolve(remoteSources)

    @get:JsonIgnore
    val auto: File?
        get() = resolve(localSources + remoteSources)

    private val localSources: List<BackupSource>
        get() = aem.tasks.named<InstanceBackup>(InstanceBackup.NAME).get().available.map { file ->
            BackupSource(BackupType.LOCAL, FileEntry.of(file)) { file }
        }

    private val remoteSources: List<BackupSource>
        get() = when {
            downloadUrl != null -> {
                val dirUrl = downloadUrl!!.substringBeforeLast("/")
                val name = downloadUrl!!.substringAfterLast("/")

                listOf(
                    BackupSource(BackupType.REMOTE, FileEntry(name)) {
                        File(downloadDir, name).apply {
                            aem.fileTransfer.downloadFrom(dirUrl, name, this)
                        }
                    }
                )
            }
            uploadUrl != null -> {
                aem.fileTransfer.list(uploadUrl!!).map { file ->
                    BackupSource(BackupType.REMOTE, file) {
                        File(downloadDir, file.name).apply {
                            aem.fileTransfer.downloadFrom(uploadUrl!!, file.name, this)
                        }
                    }
                }
            }
            else -> listOf()
        }

    private fun resolve(sources: List<BackupSource>): File? = sources.run { selector(this) }?.file
}