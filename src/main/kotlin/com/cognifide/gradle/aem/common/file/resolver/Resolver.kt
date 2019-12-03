package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.DependencyOptions
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.resolve.ResolveFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer
import com.google.common.hash.HashCode
import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.InvalidUserDataException
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

    /**
     * Controls count of groups resolved in parallel.
     */
    @Internal
    var parallelLevel = aem.props.int("resolver.parallelLevel") ?: 3

    /**
     * Files respected when searching for recent local files.
     */
    @Input
    var localFilePatterns = aem.props.list("resolver.localFilePatterns") ?: listOf("**/*.zip", "**/*.jar")

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
        return allGroups(filter).flatMap { it.files }
    }

    fun allGroups(filter: G.() -> Boolean): List<G> = groups.filter(filter).apply {
        aem.progress {
            step = "Resolving files"
            total = size.toLong()

            if (parallelLevel <= 1) {
                forEach { group ->
                    increment("Group '${group.name}'") { group.resolve() }
                }
            } else {
                val (parallel, sequential) = partition { it.parallelable }
                aem.parallel.poolEach(parallelLevel, "resolver", parallel) { group ->
                    increment("Group '${group.name}'") { group.resolve() }
                }
                sequential.forEach { group ->
                    increment("Group '${group.name}'") { group.resolve() }
                }
            }
        }
    }

    fun group(name: String): G {
        return groupsDefined.find { it.name == name }
                ?: throw FileException("File group '$name' is not defined.")
    }

    /**
     * Resolve file in case of various type of specified value: file, url to file, dependency notation, project dependency.
     */
    @Suppress("NestedBlockDepth")
    fun get(value: Any): FileResolution = aem.fileTransfer.run {
        try {
            // local file
            useLocal(project.file(value))
        } catch (e: InvalidUserDataException) {
            // files resolved using protocols
            if (value is String) {
                handling(value).run {
                    if (this is ResolveFileTransfer) { // support for special protocol 'resolve'
                        useLocal(resolve.resolve(value), parallelable)
                    } else { // other protocols like 'http', 'sftp', 'smb'
                        resolveFileUrl(value, parallelable) { downloadUsing(this, value, it) }
                    }
                }
            } else {
                // cross project / build dependencies
                useLocal(resolve.resolve(value), false)
            }
        }
    }

    /**
     * Resolve file by dependency notation using defined Gradle repositories (Maven, Ivy etc).
     */
    fun resolve(dependencyNotation: String): FileResolution = get(dependencyNotation as Any)

    /**
     * Resolve file using defined Gradle repositories (Maven, Ivy etc).
     */
    fun resolve(dependencyOptions: DependencyOptions.() -> Unit) = resolve(DependencyOptions().apply(dependencyOptions).notation)

    /**
     * Download files from same URL using automatically determined file transfer (HTTP, SFTP, SMB, URL, local file system).
     */
    fun download(urlDir: String, vararg fileNames: String) = download(urlDir, fileNames.asIterable())

    /**
     * Download files from same URL using automatically determined file transfer (HTTP, SFTP, SMB, URL, local file system).
     */
    fun download(urlDir: String, fileNames: Iterable<String>) = fileNames.map { fileName ->
        download("$urlDir/$fileName")
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
    fun download(url: String): FileResolution = get(url as Any)

    /**
     * Download file using HTTP file transfer with custom settings (like basic auth credentials).
     *
     * Use only when using more than one remote HTTP server to download files.
     */
    fun downloadHttp(url: String, options: HttpFileTransfer.() -> Unit): FileResolution {
        return resolveFileUrl(url, true) { aem.httpFile { options(); download(url, it) } }
    }

    /**
     * Download file using SFTP file transfer with custom settings (different credentials).
     *
     * Use only when using more than one remote SFTP server to download files.
     */
    fun downloadSftp(url: String, options: SftpFileTransfer.() -> Unit): FileResolution {
        return resolveFileUrl(url, true) { aem.sftpFile { options(); download(url, it) } }
    }

    /**
     * Download file using SMB file transfer with custom settings (different credentials, domain).
     *
     * Use only when using more than one remote SMB server to download files.
     */
    fun downloadSmb(url: String, options: SmbFileTransfer.() -> Unit): FileResolution {
        return resolveFileUrl(url, true) { aem.smbFile { options(); download(url, it) } }
    }

    /**
     * Use local file directly (without copying).
     */
    fun useLocal(path: String): FileResolution = useLocal(project.file(path))

    /**
     * Use local file directly (without copying).
     */
    fun useLocal(sourceFile: File): FileResolution = useLocal(sourceFile, true)

    /**
     * Use local file from directory or file when not found any.
     */
    fun useLocalBy(dir: Any, filePatterns: Iterable<String>, selector: (Iterable<File>).() -> File?): FileResolution? {
        return resolveFile(listOf(dir, filePatterns), true) {
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

    operator fun String.invoke(value: Any) = group(this) { get(value) }

    operator fun String.invoke(url: String, vararg fileNames: String) = group(this) {
        when {
            fileNames.isEmpty() -> download(url)
            else -> download(url, fileNames.asIterable())
        }
    }

    operator fun String.invoke(configurer: Resolver<G>.() -> Unit) = group(this, configurer)

    /**
     * Shorthand for creating named group with single file only to be downloaded.
     */
    fun group(name: String, downloadUrl: String) = group(name) { download(downloadUrl) }

    abstract fun createGroup(name: String): G

    private fun resolveFile(hash: Any, parallel: Boolean, resolver: (FileResolution) -> File): FileResolution {
        val id = HashCode.fromInt(HashCodeBuilder().append(hash).toHashCode()).toString()
        if (!parallel) {
            groupCurrent.parallelable = false
        }

        return groupCurrent.resolve(id, resolver)
    }

    private fun resolveFileUrl(url: String, parallel: Boolean, resolver: (File) -> Unit): FileResolution {
        return resolveFile(url, parallel) { File(it.dir, FilenameUtils.getName(url)).apply { resolver(this) } }
    }

    private fun useLocal(sourceFile: File, parallel: Boolean) = resolveFile(sourceFile.absolutePath, parallel) { sourceFile }

    companion object {
        const val GROUP_DEFAULT = "default"
    }
}
