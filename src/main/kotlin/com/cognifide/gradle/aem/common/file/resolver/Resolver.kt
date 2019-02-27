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

    private val groupsDefined = mutableListOf<G>().apply { add(groupDefault) }

    val groups: List<G>
        get() = groupsDefined.filter { it.resolutions.isNotEmpty() }

    protected open fun resolve(hash: Any, resolver: (FileResolution) -> File): FileResolution {
        val id = HashCode.fromInt(HashCodeBuilder().append(hash).toHashCode()).toString()

        return groupCurrent.resolve(id, resolver)
    }

    fun outputDirs(filter: G.() -> Boolean = { true }): List<File> {
        return groups.filter(filter).flatMap { it.dirs }
    }

    fun allFiles(filter: G.() -> Boolean = { true }): List<File> {
        return aem.parallel.map(groups.filter(filter)) { it.files }.flatten()
    }

    fun resolveGroups(filter: G.() -> Boolean): List<G> {
        return aem.parallel.pool(PARALLEL_POOL_SIZE, PARALLEL_POOL_NAME, groups.filter(filter)) { it.files; it }
    }

    fun group(name: String): G {
        return groupsDefined.find { it.name == name }
                ?: throw FileException("File group '$name' is not defined.")
    }

    fun dependency(notation: Any): FileResolution {
        return resolve(notation) {
            val configName = "fileResolver_dependency_${HashCodeBuilder().append(notation).append(downloadDir).build()}"
            val configOptions: (Configuration) -> Unit = { it.isTransitive = false }
            val config = project.configurations.create(configName, configOptions)

            project.dependencies.add(config.name, notation)
            config.singleFile
        }
    }

    fun dependency(dependencyOptions: DependencyOptions.() -> Unit): FileResolution {
        return dependency(DependencyOptions.of(project.dependencies, dependencyOptions))
    }

    fun url(url: String): FileResolution {
        return when {
            SftpFileDownloader.handles(url) -> downloadSftpAuth(url)
            SmbFileDownloader.handles(url) -> downloadSmbAuth(url)
            HttpFileDownloader.handles(url) -> downloadHttpAuth(url)
            UrlFileDownloader.handles(url) -> downloadUrl(url)
            else -> local(url)
        }
    }

    fun downloadSftp(url: String): FileResolution {
        return resolve(url) { resolution ->
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

    fun downloadSftp(url: String, sftpOptions: SftpFileDownloader.() -> Unit = {}): FileResolution {
        return resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                SftpFileDownloader(project)
                        .apply(sftpOptions)
                        .download(url, file)
            }
        }
    }

    fun downloadSftpAuth(url: String, username: String? = null, password: String? = null, hostChecking: Boolean? = null): FileResolution {
        return downloadSftp(url) {
            this.username = username ?: aem.config.resolverOptions.sftpUsername
            this.password = password ?: aem.config.resolverOptions.sftpPassword
            this.hostChecking = hostChecking ?: aem.config.resolverOptions.sftpHostChecking ?: false
        }
    }

    fun downloadSmb(url: String, smbOptions: SmbFileDownloader.() -> Unit = {}): FileResolution {
        return resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                SmbFileDownloader(project)
                        .apply(smbOptions)
                        .download(url, file)
            }
        }
    }

    fun downloadSmbAuth(url: String, domain: String? = null, username: String? = null, password: String? = null): FileResolution {
        return downloadSmb(url) {
            this.domain = domain ?: aem.config.resolverOptions.smbDomain
            this.username = username ?: aem.config.resolverOptions.smbUsername
            this.password = password ?: aem.config.resolverOptions.smbPassword
        }
    }

    fun downloadHttp(url: String, httpOptions: HttpClient.() -> Unit = {}): FileResolution {
        return resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                with(HttpFileDownloader(project)) {
                    client(httpOptions)
                    download(url, file)
                }
            }
        }
    }

    fun downloadHttpAuth(url: String, user: String? = null, password: String? = null, ignoreSsl: Boolean? = null): FileResolution {
        return downloadHttp(url) {
            basicUser = user ?: aem.config.resolverOptions.httpUsername ?: ""
            basicPassword = password ?: aem.config.resolverOptions.httpPassword ?: ""
            connectionIgnoreSsl = ignoreSsl ?: aem.config.resolverOptions.httpConnectionIgnoreSsl ?: true
        }
    }

    fun downloadUrl(url: String): FileResolution {
        return resolve(url) { resolution ->
            download(url, resolution.dir) { file ->
                UrlFileDownloader(project).download(url, file)
            }
        }
    }

    fun local(path: String): FileResolution {
        return local(project.file(path))
    }

    fun local(sourceFile: File): FileResolution {
        return resolve(sourceFile.absolutePath) { sourceFile }
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

    companion object {
        const val GROUP_DEFAULT = "default"

        const val DOWNLOAD_LOCK = "download.lock"

        const val PARALLEL_POOL_SIZE = 3

        const val PARALLEL_POOL_NAME = "aem-resolver"
    }
}