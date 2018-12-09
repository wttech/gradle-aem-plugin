package com.cognifide.gradle.aem.common.file.resolver

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.DependencyOptions
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.downloader.HttpFileDownloader
import com.cognifide.gradle.aem.common.file.downloader.SftpFileDownloader
import com.cognifide.gradle.aem.common.file.downloader.SmbFileDownloader
import com.cognifide.gradle.aem.common.file.downloader.UrlFileDownloader
import com.cognifide.gradle.aem.common.http.HttpClient
import com.google.common.hash.HashCode
import java.io.File
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

    private val groups = mutableListOf<G>().apply { add(groupDefault) }

    protected open fun resolve(hash: Any, resolver: (FileResolution) -> File) {
        val id = HashCode.fromInt(HashCodeBuilder().append(hash).toHashCode()).toString()

        groupCurrent.resolve(id, resolver)
    }

    fun outputDirs(filter: (String) -> Boolean = { true }): List<File> {
        return filterGroups(filter).flatMap { it.dirs }
    }

    fun allFiles(filter: (String) -> Boolean = { true }): List<File> {
        return filterGroups(filter).flatMap { it.files }
    }

    fun group(name: String): G {
        return groups.find { it.name == name }
                ?: throw FileException("File group '$name' is not defined.")
    }

    fun filterGroups(filter: String): List<G> {
        return filterGroups { Patterns.wildcard(it, filter) }
    }

    fun filterGroups(filter: (String) -> Boolean): List<G> {
        return groups.filter { filter(it.name) }.filter { it.resolutions.isNotEmpty() }
    }

    fun dependency(notation: Any) {
        resolve(notation) {
            val configName = "fileResolver_dependency_${HashCodeBuilder().append(notation).append(downloadDir).build()}"
            val configOptions: (Configuration) -> Unit = { it.isTransitive = false }
            val config = project.configurations.create(configName, configOptions)

            project.dependencies.add(config.name, notation)
            config.singleFile
        }
    }

    fun dependency(dependencyOptions: DependencyOptions.() -> Unit) {
        dependency(DependencyOptions.of(project.dependencies, dependencyOptions))
    }

    fun url(url: String) {
        when {
            SftpFileDownloader.handles(url) -> downloadSftpAuth(url)
            SmbFileDownloader.handles(url) -> downloadSmbAuth(url)
            HttpFileDownloader.handles(url) -> downloadHttpAuth(url)
            UrlFileDownloader.handles(url) -> downloadUrl(url)
            else -> local(url)
        }
    }

    fun downloadSftp(url: String) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                SftpFileDownloader(project).download(url, file)
            }
        }
    }

    private fun download(url: String, targetDir: File, downloader: (File) -> Unit): File {
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

    fun downloadSftp(url: String, sftpOptions: SftpFileDownloader.() -> Unit = {}) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                SftpFileDownloader(project)
                        .apply(sftpOptions)
                        .download(url, file)
            }
        }
    }

    fun downloadSftpAuth(url: String, username: String? = null, password: String? = null, hostChecking: Boolean? = null) {
        downloadSftp(url) {
            this.username = username ?: aem.props.prop("aem.resolver.sftp.username")
            this.password = password ?: aem.props.prop("aem.resolver.sftp.password")
            this.hostChecking = hostChecking ?: aem.props.boolean("aem.resolver.sftp.hostChecking") ?: false
        }
    }

    fun downloadSmb(url: String, smbOptions: SmbFileDownloader.() -> Unit = {}) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                SmbFileDownloader(project)
                        .apply(smbOptions)
                        .download(url, file)
            }
        }
    }

    fun downloadSmbAuth(url: String, domain: String? = null, username: String? = null, password: String? = null) {
        downloadSmb(url) {
            this.domain = domain ?: aem.props.prop("aem.resolver.smb.domain")
            this.username = username ?: aem.props.prop("aem.resolver.smb.username")
            this.password = password ?: aem.props.prop("aem.resolver.smb.password")
        }
    }

    fun downloadHttp(url: String, httpOptions: HttpClient.() -> Unit = {}) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                with(HttpFileDownloader(project)) {
                    client(httpOptions)
                    download(url, file)
                }
            }
        }
    }

    fun downloadHttpAuth(url: String, user: String? = null, password: String? = null, ignoreSsl: Boolean? = null) {
        downloadHttp(url) {
            basicUser = user ?: aem.props.string("aem.resolver.http.username") ?: ""
            basicPassword = password ?: aem.props.string("aem.resolver.http.password") ?: ""
            connectionIgnoreSsl = ignoreSsl ?: aem.props.boolean("aem.resolver.http.connectionIgnoreSsl") ?: true
        }
    }

    fun downloadUrl(url: String) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                UrlFileDownloader(project).download(url, file)
            }
        }
    }

    fun local(path: String) {
        local(project.file(path))
    }

    fun local(sourceFile: File) {
        resolve(sourceFile.absolutePath) { sourceFile }
    }

    fun config(configurer: G.() -> Unit) {
        groupCurrent.apply(configurer)
    }

    @Synchronized
    fun group(name: String, configurer: Resolver<G>.() -> Unit) {
        groupCurrent = groups.find { it.name == name } ?: createGroup(name).apply { groups.add(this) }
        this.apply(configurer)
        groupCurrent = groupDefault
    }

    abstract fun createGroup(name: String): G

    companion object {
        const val GROUP_DEFAULT = "default"

        const val DOWNLOAD_LOCK = "download.lock"
    }
}