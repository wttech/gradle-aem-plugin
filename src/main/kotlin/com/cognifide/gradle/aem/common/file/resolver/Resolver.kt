package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.DependencyOptions
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer
import com.cognifide.gradle.aem.common.utils.Formats
import com.google.common.hash.HashCode
import java.io.File
import java.util.*
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Internal
import org.gradle.util.GFileUtils

/**
 * File downloader with groups supporting files from multiple sources: local and remote (SFTP, SMB, HTTP).
 */
abstract class Resolver<G : FileGroup>(
    @get:Internal
val aem: AemExtension,

    @get:Internal
val downloadDir: File
) {
    private val project = aem.project

    private val groupDefault = this.createGroup(GROUP_DEFAULT)

    private var groupCurrent = groupDefault

    private val groupsDefined = mutableListOf<G>().apply { add(groupDefault) }

    @get:Internal
    val groups: List<G>
        get() = groupsDefined.filter { it.resolutions.isNotEmpty() }

    fun outputDirs(filter: G.() -> Boolean = { true }): List<File> {
        return groups.filter(filter).flatMap { it.dirs }
    }

    fun allFiles(filter: G.() -> Boolean = { true }): List<File> {
        return resolveGroups(filter).flatMap { it.files }
    }

    fun resolveGroups(filter: G.() -> Boolean = { true }): List<G> {
        return groups.filter(filter).onEach { it.files }
    }

    fun group(name: String): G {
        return groupsDefined.find { it.name == name }
                ?: throw FileException("File group '$name' is not defined.")
    }

    /**
     * Resolve file by dependency notation using defined Gradle repositories (Maven, Ivy etc).
     */
    fun resolve(notation: Any): FileResolution {
        return groupFile(notation) {
            val configName = "fileResolver_dependency_${UUID.randomUUID()}"
            val configOptions: (Configuration) -> Unit = { it.isTransitive = false }
            val config = project.configurations.create(configName, configOptions)

            project.dependencies.add(config.name, notation)
            config.singleFile
        }
    }

    /**
     * Resolve file using defined Gradle repositories (Maven, Ivy etc).
     */
    fun resolve(dependencyOptions: DependencyOptions.() -> Unit): FileResolution {
        return resolve(DependencyOptions.of(project.dependencies, dependencyOptions))
    }

    /**
     * Download file using automatically determined file transfer (HTTP, SFTP, SMB, URL, local file system).
     *
     * Same global settings (like basic auth credentials of HTTP) of each particular file transfer will be used
     * for all files downloaded. This shorthand method assumes that mostly only single HTTP / SFTP / SMB server
     * will be used to download all files.
     *
     * To use many remote servers with different settings, simply use dedicated methods 'download[Http/Sftp/Smb]'
     * when declaring each file to be downloaded.
     */
    fun download(url: String): FileResolution = groupDownload(url) { aem.fileTransfer.download(url, it) }

    /**
     * Download file using HTTP file transfer with custom settings (like basic auth credentials).
     *
     * Use only when using more than one remote HTTP server to download files.
     */
    fun downloadHttp(url: String, options: HttpFileTransfer.() -> Unit): FileResolution {
        return groupDownload(url) { aem.httpFile { options(); download(url, it) } }
    }

    /**
     * Download file using SFTP file transfer with custom settings (different credentials).
     *
     * Use only when using more than one remote SFTP server to download files.
     */
    fun downloadSftp(url: String, options: SftpFileTransfer.() -> Unit): FileResolution {
        return groupDownload(url) { aem.sftpFile { options(); download(url, it) } }
    }

    /**
     * Download file using SMB file transfer with custom settings (different credentials, domain).
     *
     * Use only when using more than one remote SMB server to download files.
     */
    fun downloadSmb(url: String, options: SmbFileTransfer.() -> Unit): FileResolution {
        return groupDownload(url) { aem.smbFile { options(); download(url, it) } }
    }

    /**
     * Use local file directly (without copying).
     */
    fun useLocal(path: String): FileResolution {
        return useLocal(project.file(path))
    }

    /**
     * Use local file directly (without copying).
     */
    fun useLocal(sourceFile: File): FileResolution {
        return groupFile(sourceFile.absolutePath) { sourceFile }
    }

    fun config(configurer: G.() -> Unit) {
        groupCurrent.apply(configurer)
    }

    @Synchronized
    fun group(name: String, configurer: Resolver<G>.() -> Unit) {
        groupCurrent = groupsDefined.find { it.name == name } ?: createGroup(name).apply { groupsDefined.add(this) }
        this.apply(configurer)
        groupCurrent = groupDefault
    }

    abstract fun createGroup(name: String): G

    protected open fun groupFile(hash: Any, resolver: (FileResolution) -> File): FileResolution {
        val id = HashCode.fromInt(HashCodeBuilder().append(hash).toHashCode()).toString()

        return groupCurrent.resolve(id, resolver)
    }

    private fun downloadFile(url: String, targetDir: File, downloader: (File) -> Unit): File {
        GFileUtils.mkdirs(targetDir)

        val file = File(targetDir, FilenameUtils.getName(url))
        val lock = File(targetDir, DOWNLOAD_LOCK)
        if (!lock.exists() && file.exists()) {
            file.delete()
        }

        if (!file.exists()) {
            downloader(file)
            lock.printWriter().use { it.print(Formats.toJson(mapOf("downloaded" to Formats.date()))) }
        }

        return file
    }

    private fun groupDownload(url: String, downloader: (File) -> Unit): FileResolution {
        return groupFile(url) { resolution ->
            downloadFile(url, resolution.dir) { file ->
                downloader(file)
            }
        }
    }

    companion object {
        const val GROUP_DEFAULT = "default"

        const val DOWNLOAD_LOCK = "download.lock"
    }
}