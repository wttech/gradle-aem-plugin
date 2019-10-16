package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.DependencyOptions
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer
import com.google.common.hash.HashCode
import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

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

    @get:Internal
    val outputDirs: List<File>
        get() = outputDirs { true }

    fun outputDirs(filter: G.() -> Boolean): List<File> {
        return groups.filter(filter).flatMap { it.dirs }
    }

    @get:Internal
    val allFiles: List<File>
        get() = allFiles { true }

    fun allFiles(filter: G.() -> Boolean): List<File> {
        return resolveGroups(filter).flatMap { it.files }
    }

    fun resolveGroups(filter: G.() -> Boolean): List<G> {
        return groups.filter(filter).onEach { it.files }
    }

    fun group(name: String): G {
        return groupsDefined.find { it.name == name }
                ?: throw FileException("File group '$name' is not defined.")
    }

    /**
     * Resolve file by dependency notation using defined Gradle repositories (Maven, Ivy etc).
     */
    fun resolve(dependencyNotation: String): FileResolution = resolve(dependencyNotation as Any)

    /**
     * Resolve file using defined Gradle repositories (Maven, Ivy etc).
     */
    fun resolve(dependencyOptions: DependencyOptions.() -> Unit) = resolve(DependencyOptions.create(aem, dependencyOptions))

    private fun resolve(dependencyNotation: Any): FileResolution = resolveFile(dependencyNotation) {
        project.configurations.detachedConfiguration(project.dependencies.create(dependencyNotation)).singleFile
    }

    /**
     * Download files from same URL using automatically determined file transfer (HTTP, SFTP, SMB, URL, local file system).
     */
    fun download(urlDir: String, vararg fileNames: String) = download(urlDir, fileNames.asIterable())

    /**
     * Download files from same URL using automatically determined file transfer (HTTP, SFTP, SMB, URL, local file system).
     */
    fun download(urlDir: String, fileNames: Iterable<String>) = fileNames.map {
        fileName -> download("$urlDir/$fileName")
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
    fun download(url: String): FileResolution = resolveFileUrl(url) { aem.fileTransfer.download(url, it) }

    /**
     * Download file using HTTP file transfer with custom settings (like basic auth credentials).
     *
     * Use only when using more than one remote HTTP server to download files.
     */
    fun downloadHttp(url: String, options: HttpFileTransfer.() -> Unit): FileResolution {
        return resolveFileUrl(url) { aem.httpFile { options(); download(url, it) } }
    }

    /**
     * Download file using SFTP file transfer with custom settings (different credentials).
     *
     * Use only when using more than one remote SFTP server to download files.
     */
    fun downloadSftp(url: String, options: SftpFileTransfer.() -> Unit): FileResolution {
        return resolveFileUrl(url) { aem.sftpFile { options(); download(url, it) } }
    }

    /**
     * Download file using SMB file transfer with custom settings (different credentials, domain).
     *
     * Use only when using more than one remote SMB server to download files.
     */
    fun downloadSmb(url: String, options: SmbFileTransfer.() -> Unit): FileResolution {
        return resolveFileUrl(url) { aem.smbFile { options(); download(url, it) } }
    }

    /**
     * Use local file directly (without copying).
     */
    fun useLocal(path: String): FileResolution = useLocal(project.file(path))

    /**
     * Use local file directly (without copying).
     */
    fun useLocal(sourceFile: File): FileResolution = resolveFile(sourceFile.absolutePath) { sourceFile }

    /**
     * Use local file from directory or file when not found any.
     */
    fun useLocalBy(dir: Any, filePatterns: Iterable<String>, selector: (Iterable<File>).() -> File?): FileResolution? {
        return resolveFile(listOf(dir, filePatterns)) {
            aem.project.fileTree(dir) { it.include(filePatterns) }.run(selector)
                    ?: throw FileException("Cannot find any local file under directory '$dir' matching file pattern '$filePatterns'!")
        }
    }

    /**
     * Use local file from directory or file when not found any.
     */
    fun useLocalBy(dir: Any, selector: (Iterable<File>).() -> File?) = useLocalBy(dir, localFilePatterns, selector)

    /**
     * Use local file with name being highest version located in directory or fail when not found any.
     * Highest version is determined by descending alphanumeric sorting.
     */
    fun useLocalLastNamed(dir: Any) = useLocalBy(dir) { maxBy { it.name } }

    /**
     * Use last modified local file located in directory or fail when not found any.
     */
    fun useLocalLastModified(dir: Any) = useLocalBy(dir) { maxBy { it.lastModified() } }

    /**
     * Use last modified local file located in directory or fail when not found any.
     */
    fun useLocalRecent(dir: Any) = useLocalLastModified(dir)

    /**
     * Files respected when searching for recent local files.
     */
    @Input
    var localFilePatterns = listOf("**/*.zip", "**/*.jar")

    /**
     * Customize configuration for particular file group.
     */
    fun config(configurer: G.() -> Unit) {
        groupCurrent.apply(configurer)
    }

    @Synchronized
    fun group(name: String, configurer: Resolver<G>.() -> Unit) {
        groupCurrent = groupsDefined.find { it.name == name } ?: createGroup(name).apply { groupsDefined.add(this) }
        this.apply(configurer)
        groupCurrent = groupDefault
    }

    operator fun String.invoke(configurer: Resolver<G>.() -> Unit) = group(this, configurer)

    /**
     * Shorthand for creating named group with single file only to be downloaded.
     */
    fun group(name: String, downloadUrl: String) = group(name) { download(downloadUrl) }

    abstract fun createGroup(name: String): G

    private fun resolveFile(hash: Any, resolver: (FileResolution) -> File): FileResolution {
        val id = HashCode.fromInt(HashCodeBuilder().append(hash).toHashCode()).toString()

        return groupCurrent.resolve(id, resolver)
    }

    private fun resolveFileUrl(url: String, resolver: (File) -> Unit): FileResolution {
        return resolveFile(url) { File(it.dir, FilenameUtils.getName(url)).apply { resolver(this) } }
    }

    companion object {
        const val GROUP_DEFAULT = "default"
    }
}
