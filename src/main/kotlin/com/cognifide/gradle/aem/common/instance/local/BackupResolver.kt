package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.instance.tasks.InstanceBackup
import com.cognifide.gradle.common.file.FileException
import com.cognifide.gradle.common.file.transfer.FileEntry
import com.cognifide.gradle.common.utils.Formats
import java.io.File

class BackupResolver(private val aem: AemExtension) {

    private val common = aem.common

    /**
     * URL to remote directory in which backup files are stored.
     */
    val uploadUrl = aem.obj.string {
        aem.prop.string("localInstance.backup.uploadUrl")?.let { set(it) }
    }

    /**
     * URL to remote backup file.
     */
    val downloadUrl = aem.obj.string {
        aem.prop.string("localInstance.backup.downloadUrl")?.let { set(it) }
    }

    /**
     * Backup file from any source (local & remote sources).
     */
    val any: File? get() = resolve(localSources + remoteSources)

    /**
     * Directory storing locally created backup files.
     */
    val localDir = aem.obj.dir {
        convention(aem.obj.buildDir("${InstanceBackup.NAME}/${BackupType.LOCAL.dirName}"))
        aem.prop.file("localInstance.backup.localDir")?.let { set(it) }
    }

    /**
     * Backup file from local source.
     */
    val local: File? get() = resolve(localSources)

    /**
     * Directory storing downloaded remote backup files.
     */
    val remoteDir = aem.obj.dir {
        convention(aem.obj.projectDir("${InstanceBackup.NAME}/${BackupType.REMOTE.dirName}"))
        aem.prop.file("localInstance.backup.remoteDir")?.let { set(it) }
    }

    /**
     * Backup file from remote source.
     */
    val remote: File? get() = resolve(remoteSources)

    /**
     * File suffix indicating instance backup file.
     */
    val suffix = aem.obj.string {
        convention(SUFFIX_DEFAULT)
        aem.prop.string("localInstance.backup.suffix")?.let { set(it) }
    }

    /**
     * Defines backup file naming rule.
     * Must be in sync with selector rule.
     */
    var namer: () -> String = { "${aem.project.rootProject.name}-${ Formats.dateFileName()}-${aem.project.version}$suffix" }

    /**
     * Defines backup source selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup.
     * Also by default, file names are sorted lexically / descending. If same name on local & remote source found, local has precedence.
     * Still, this callback allows to customize order to be used.
     */
    var selector: Collection<BackupSource>.() -> BackupSource? = {
        val name = aem.prop.string("localInstance.backup.name") ?: ""
        when {
            name.isNotBlank() -> firstOrNull { it.fileEntry.name == name }
            else -> firstOrNull()
        }
    }

    private fun resolve(sources: List<BackupSource>): File? = sources
            .filter { it.fileEntry.name.endsWith(suffix.get()) }
            .sortedWith(compareByDescending<BackupSource> { it.fileEntry.name }.thenBy { it.type.ordinal })
            .run { selector(this) }?.file

    private val localSources: List<BackupSource>
        get() = (localDir.get().asFile.listFiles { _, name -> name.endsWith(suffix.get()) } ?: arrayOf()).map { file ->
            BackupSource(BackupType.LOCAL, FileEntry.of(file)) { file }
        }

    private val remoteSources: List<BackupSource>
        get() = when {
            !downloadUrl.orNull.isNullOrBlank() -> {
                val dirUrl = downloadUrl.get().substringBeforeLast("/")
                val name = downloadUrl.get().substringAfterLast("/")

                val fileEntry = try {
                    common.fileTransfer.stat(dirUrl, name) // 'stat' may be unsupported
                } catch (e: FileException) {
                    aem.logger.debug("Cannot check instance backup file status at URL '$dirUrl/$name'", e)
                    FileEntry(name)
                }

                if (fileEntry != null) {
                    listOf(BackupSource(BackupType.REMOTE, fileEntry) {
                        remoteDir.get().asFile.resolve(name).apply { common.fileTransfer.downloadFrom(dirUrl, name, this) }
                    })
                } else {
                    aem.logger.info("Instance backup at URL '$dirUrl/$name' is not available.")
                    listOf()
                }
            }
            !uploadUrl.orNull.isNullOrBlank() -> {
                val fileEntries = common.fileTransfer.list(uploadUrl.get())
                if (fileEntries.isEmpty()) {
                    aem.logger.info("No instance backups available at URL '$uploadUrl'.")
                }

                fileEntries.map { file ->
                    BackupSource(BackupType.REMOTE, file) {
                        remoteDir.get().asFile.resolve(file.name).apply { common.fileTransfer.downloadFrom(uploadUrl.get(), name, this) }
                    }
                }
            }
            else -> listOf()
        }

    companion object {
        const val SUFFIX_DEFAULT = ".backup.zip"
    }
}
