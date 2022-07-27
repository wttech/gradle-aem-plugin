package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.aem.common.instance.LocalInstanceException
import com.cognifide.gradle.aem.common.instance.LocalInstanceManager
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.common.file.FileException
import com.cognifide.gradle.common.file.transfer.FileEntry
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.onEachApply
import java.io.File

class BackupManager(private val manager: LocalInstanceManager) {

    private val aem = manager.aem

    private val common = aem.common

    private val logger = aem.logger

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
    val any: File? get() = resolve(localSources + remoteSources)?.file

    /**
     * Backup file specified explicitly.
     */
    val localFile = aem.obj.file {
        aem.prop.file("localInstance.backup.localFile")?.let { set(it) }
    }

    /**
     * Directory storing locally created backup files.
     */
    val localDir = aem.obj.dir {
        convention(manager.rootDir.dir("$OUTPUT_DIR/${BackupType.LOCAL.dirName}"))
        aem.prop.file("localInstance.backup.localDir")?.let { set(it) }
    }

    /**
     * Determines the number of local backup ZIP files stored.
     * After creating new backups, old ones are auto-removed.
     */
    val localCount = aem.obj.int {
        convention(3)
        aem.prop.int("localInstance.backup.localCount")?.let { set(it) }
    }

    /**
     * Backup file from local source.
     */
    val local: File? get() = resolve(localSources)?.file

    /**
     * Directory storing downloaded remote backup files.
     */
    val remoteDir = aem.obj.dir {
        convention(manager.rootDir.dir("$OUTPUT_DIR/${BackupType.REMOTE.dirName}"))
        aem.prop.file("localInstance.backup.remoteDir")?.let { set(it) }
    }

    /**
     * Backup file from remote source.
     */
    val remote: File? get() = resolve(remoteSources)?.file

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
    fun namer(provider: () -> String) {
        this.namer = provider
    }

    private var namer: () -> String = {
        val parts = mutableListOf<String>().apply {
            add(aem.project.rootProject.name)
            add(Formats.dateFileName())
            val version = aem.project.version.toString()
            if (version.isNotBlank() && version != "unspecified") {
                add(version)
            }
        }
        "${parts.joinToString("-")}${suffix.get()}"
    }

    /**
     * Get newly created file basing on namer rule.
     */
    val namedFile: File get() = localDir.get().asFile.resolve(namer())

    /**
     * Defines backup source selection rule.
     *
     * By default takes desired backup by name (if provided) or takes most recent backup.
     * Also by default, file names are sorted lexically / descending. If same name on local & remote source found, local has precedence.
     * Still, this callback allows to customize order to be used.
     */
    fun selector(selector: Collection<BackupSource>.() -> BackupSource?) {
        this.selector = selector
    }

    private var selector: Collection<BackupSource>.() -> BackupSource? = {
        val name = aem.prop.string("localInstance.backup.name") ?: ""
        when {
            name.isNotBlank() -> firstOrNull { it.fileEntry.name == name }
            localFileSource != null -> firstOrNull { it == localFileSource }
            else -> firstOrNull()
        }
    }

    private fun resolve(sources: List<BackupSource>): BackupSource? = sources
        .filter { it.fileEntry.name.endsWith(suffix.get()) }
        .sortedWith(compareByDescending<BackupSource> { it.fileEntry.name }.thenBy { it.type.ordinal })
        .run { selector(this) }

    private val localSources: List<BackupSource> get() = listOfNotNull(localFileSource) + localDirSources

    private val localFileSource: BackupSource? get() = localFile.orNull?.asFile?.let { BackupSource(BackupType.LOCAL, FileEntry.of(it)) { it } }

    private val localDirSources: List<BackupSource>
        get() = (localDir.get().asFile.listFiles { _, name -> name.endsWith(suffix.get()) } ?: arrayOf()).map { file ->
            BackupSource(BackupType.LOCAL, FileEntry.of(file)) { file }
        }

    private val remoteSources: List<BackupSource>
        get() = when {
            !downloadUrl.orNull.isNullOrBlank() -> listOfNotNull(remoteDownloadSource)
            !uploadUrl.orNull.isNullOrBlank() -> remoteUploadSources
            else -> listOf()
        }

    private val remoteDownloadSource: BackupSource?
        get() {
            val dirUrl = downloadUrl.get().substringBeforeLast("/")
            val name = downloadUrl.get().substringAfterLast("/")

            val fileEntry = try {
                common.fileTransfer.stat(dirUrl, name) // 'stat' may be unsupported
            } catch (e: FileException) {
                logger.info("Cannot check instance backup file status at URL '$dirUrl/$name'! Cause: ${e.message}")
                logger.debug("Actual error", e)
                FileEntry(name)
            }

            return if (fileEntry != null) {
                BackupSource(BackupType.REMOTE, fileEntry) {
                    remoteDir.get().asFile.resolve(name).apply { common.fileTransfer.downloadFrom(dirUrl, name, this) }
                }
            } else {
                logger.info("Instance backup at URL '$dirUrl/$name' is not available.")
                null
            }
        }

    private val remoteUploadSources: List<BackupSource>
        get() {
            val fileEntries = common.fileTransfer.list(uploadUrl.get())
            if (fileEntries.isEmpty()) {
                logger.info("No instance backups available at URL '${uploadUrl.get()}'.")
            }

            return fileEntries.map { file ->
                BackupSource(BackupType.REMOTE, file) {
                    remoteDir.get().asFile.resolve(file.name).apply { common.fileTransfer.downloadFrom(uploadUrl.get(), name, this) }
                }
            }
        }

    fun create(instances: Collection<LocalInstance>) = namedFile.apply { create(this, instances) }

    fun create(file: File, instances: Collection<LocalInstance>) {
        val uncreated = instances.filter { !it.created }
        if (uncreated.isNotEmpty()) {
            throw LocalInstanceException("Cannot create local instance backup, because there are instances not yet created: ${uncreated.names}")
        }

        val running = instances.filter { it.status.running }
        if (running.isNotEmpty()) {
            throw LocalInstanceException("Cannot create local instance backup, because there are instances still running: ${running.names}")
        }

        val zip = common.zip(file)

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Backing up instance '$name'") {
                    common.progress {
                        updater { update("Adding files to '${file.name}' (${Formats.fileSize(file)})") }
                        zip.addDir(dir)
                    }
                }
            }
        }
    }

    fun upload(backupZip: File, verbose: Boolean): Boolean {
        val dirUrl = uploadUrl.orNull
        if (dirUrl.isNullOrBlank()) {
            val message = "Skipped uploading local instance backup as of URL is not defined."
            if (verbose) {
                throw LocalInstanceException(message)
            } else {
                logger.info(message)
                return false
            }
        }

        logger.info("Uploading local instance(s) backup file '$backupZip' to URL '$dirUrl'")
        common.fileTransfer.uploadTo(dirUrl, backupZip)

        return true
    }

    fun restore(backupZip: File, rootDir: File, instances: Collection<LocalInstance>) {
        logger.info("Restoring instances from backup ZIP '$backupZip' to directory '$rootDir'")

        rootDir.mkdirs()

        common.progress(instances.size) {
            instances.onEachApply {
                increment("Restoring instance '$name'") {
                    common.zip(backupZip).unpackDir(purposeId, rootDir)
                }
            }
        }
    }

    fun clean() {
        val preservedMax = localCount.get()
        if (preservedMax <= 0) {
            logger.info("Backups cleaning is disabled!")
            return
        }

        val cleanable = localSources.toMutableList()
        var preserved = 0
        while (cleanable.isNotEmpty() && preserved < preservedMax) {
            val recent = resolve(cleanable) ?: break
            cleanable.remove(recent)
            preserved++
        }

        if (cleanable.isEmpty()) {
            logger.info("No backups to clean!")
        } else {
            cleanable.forEach { source ->
                logger.info("Cleaning backup file ${source.file}")
                source.file.delete()
            }
        }
    }

    companion object {
        const val OUTPUT_DIR = "backup"

        const val SUFFIX_DEFAULT = ".backup.zip"
    }
}
