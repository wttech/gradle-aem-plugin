package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class BackupResolver(private val aem: AemExtension) {

    /**
     * URL to remote directory in which backup files are stored.
     */
    var uploadUrl = aem.props.string("localInstance.backup.uploadUrl")

    /**
     * URL to remote backup file.
     */
    var downloadUrl = aem.props.string("localInstance.backup.downloadUrl")

    /**
     * Backup file from any source (local & remote sources).
     */
    @get:JsonIgnore
    val any: File?
        get() = resolve(localSources + remoteSources)

    /**
     * Directory storing locally created backup files.
     */
    var localDir: File = aem.props.string("localInstance.backup.localDir")?.let { aem.project.file(it) }
            ?: aem.temporaryDir("instanceBackup/local")

    /**
     * Backup file from local source.
     */
    @get:JsonIgnore
    val local: File?
        get() = resolve(localSources)

    /**
     * Directory storing downloaded remote backup files.
     */
    var remoteDir: File = aem.props.string("localInstance.backup.remoteDir")?.let { aem.project.file(it) }
            ?: aem.temporaryDir("instanceBackup/remote")

    /**
     * Backup file from remote source.
     */
    @get:JsonIgnore
    val remote: File?
        get() = resolve(remoteSources)

    /**
     * Defines backup file naming rule.
     * Must be in sync with selector rule.
     */
    var namer: () -> String = { "${aem.project.rootProject.name}-${ Formats.dateFileName()}-${aem.project.version}.backup.zip" }

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

    private fun resolve(sources: List<BackupSource>): File? = sources.run { selector(this) }?.file

    private val localSources: List<BackupSource>
        get() = (localDir.listFiles { _, name -> name.endsWith(".backup.zip") } ?: arrayOf()).map { file ->
            BackupSource(BackupType.LOCAL, FileEntry.of(file)) { file }
        }

    private val remoteSources: List<BackupSource>
        get() = when {
            downloadUrl != null -> {
                val dirUrl = downloadUrl!!.substringBeforeLast("/")
                val name = downloadUrl!!.substringAfterLast("/")

                val fileEntry = try {
                    aem.fileTransfer.stat(dirUrl, name)
                } catch (e: FileException) {
                    aem.logger.debug("Cannot check instance backup file status at URL '$dirUrl/$name'", e)
                    FileEntry(name) // 'stat' may be unsupported by some file transfers like 'http', 'url'
                }

                if (fileEntry != null) {
                    listOf(BackupSource(BackupType.REMOTE, fileEntry) {
                        File(remoteDir, name).apply { aem.fileTransfer.downloadFrom(dirUrl, name, this) }
                    })
                } else {
                    aem.logger.info("Instance backup at URL '$dirUrl/$name' is not available.")
                    listOf()
                }
            }
            uploadUrl != null -> {
                val fileEntries = aem.fileTransfer.list(uploadUrl!!)
                if (fileEntries.isEmpty()) {
                    aem.logger.info("No instance backups available at URL '$uploadUrl'.")
                }

                fileEntries.map { file ->
                    BackupSource(BackupType.REMOTE, file) {
                        File(remoteDir, file.name).apply { aem.fileTransfer.downloadFrom(uploadUrl!!, name, this) }
                    }
                }
            }
            else -> listOf()
        }
}