package com.cognifide.gradle.aem.internal.file.resolver

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.internal.DependencyOptions
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.file.FileException
import com.cognifide.gradle.aem.internal.file.downloader.HttpFileDownloader
import com.cognifide.gradle.aem.internal.file.downloader.SftpFileDownloader
import com.cognifide.gradle.aem.internal.file.downloader.SmbFileDownloader
import com.cognifide.gradle.aem.internal.file.downloader.UrlFileDownloader
import com.google.common.hash.HashCode
import java.io.File
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Internal
import org.gradle.util.GFileUtils

/**
 * File downloader with groups supporting files from multiple sources: local and remote (SFTP, SMB, HTTP).
 */
abstract class Resolver<G : FileGroup>(
    @get:Internal
    val project: Project,

    @get:Internal
    val downloadDir: File
) {

    private val aem = AemExtension.of(project)

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

    fun downloadSftpAuth(url: String) {
        downloadSftpAuth(url, null, null)
    }

    fun downloadSftpAuth(url: String, hostChecking: Boolean?) {
        downloadSftpAuth(url, null, null, hostChecking)
    }

    fun downloadSftpAuth(url: String, username: String?, password: String?) {
        downloadSftpAuth(url, username, password, null)
    }

    fun downloadSftpAuth(url: String, username: String?, password: String?, hostChecking: Boolean?) {
        resolve(arrayOf(url, username, password, hostChecking)) { resolution ->
            download(url, resolution.dir) { file ->
                val downloader = SftpFileDownloader(project)

                downloader.username = username ?: aem.props.prop("aem.sftp.username")
                downloader.password = password ?: aem.props.prop("aem.sftp.password")
                downloader.hostChecking = hostChecking ?: aem.props.boolean("aem.sftp.hostChecking", false)

                downloader.download(url, file)
            }
        }
    }

    fun downloadSmb(url: String) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                SmbFileDownloader(project).download(url, file)
            }
        }
    }

    fun downloadSmbAuth(url: String) {
        downloadSmbAuth(url, null, null, null)
    }

    fun downloadSmbAuth(url: String, domain: String?, username: String?, password: String?) {
        resolve(arrayOf(url, domain, username, password)) { resolution ->
            download(url, resolution.dir) { file ->
                val downloader = SmbFileDownloader(project)

                downloader.domain = domain ?: aem.props.prop("aem.smb.domain")
                downloader.username = username ?: aem.props.prop("aem.smb.username")
                downloader.password = password ?: aem.props.prop("aem.smb.password")

                downloader.download(url, file)
            }
        }
    }

    fun downloadHttp(url: String) {
        resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                HttpFileDownloader(project).download(url, file)
            }
        }
    }

    fun downloadHttpAuth(url: String) {
        downloadHttpAuth(url, null, null)
    }

    fun downloadHttpAuth(url: String, ignoreSSL: Boolean?) {
        downloadHttpAuth(url, null, null, ignoreSSL)
    }

    fun downloadHttpAuth(url: String, user: String?, password: String?) {
        downloadHttpAuth(url, user, password, null)
    }

    fun downloadHttpAuth(url: String, user: String?, password: String?, ignoreSSL: Boolean?) {
        resolve(arrayOf(url, user, password, ignoreSSL)) { resolution ->
            download(url, resolution.dir) { file ->
                val downloader = HttpFileDownloader(project)

                downloader.username = user ?: aem.props.prop("aem.http.username")
                downloader.password = password ?: aem.props.prop("aem.http.password")
                downloader.ignoreSSLErrors = ignoreSSL ?: aem.props.boolean("aem.http.ignoreSSL", true)

                downloader.download(url, file)
            }
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