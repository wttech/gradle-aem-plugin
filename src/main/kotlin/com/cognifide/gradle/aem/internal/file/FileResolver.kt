package com.cognifide.gradle.aem.internal.file

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.google.common.hash.HashCode
import groovy.lang.Closure
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import org.gradle.util.GFileUtils
import java.io.File

/**
 * Generic file downloader with groups supporting files from local and remote sources (SFTP, SMB, HTTP).
 */
class FileResolver(val project: Project, val downloadDir: File) {

    companion object {
        val GROUP_DEFAULT = "default"

        val DOWNLOAD_LOCK = "download.lock"
    }

    private inner class Resolver(val id: String, val group: String, val action: (Resolver) -> Unit) {
        val dir = File("$downloadDir/$id")
    }

    private val resolvers = mutableListOf<Resolver>()

    private var group: String = GROUP_DEFAULT

    val configured: Boolean
        get() = resolvers.isNotEmpty()

    val configurationHash: Int
        get() {
            val builder = HashCodeBuilder()
            resolvers.forEach { builder.append(it.id) }

            return builder.toHashCode()
        }

    fun attach(task: DefaultTask, prop: String = "fileResolver") {
        task.outputs.dir(downloadDir)
        project.afterEvaluate {
            task.inputs.property(prop, configurationHash)
        }
    }

    fun allFiles(filter: (String) -> Boolean = { true }, resolve: Boolean = true): List<File> {
        return resolveFiles(filter, resolve).map { it.dir }
    }

    fun groupedFiles(filter: (String) -> Boolean = { true }, resolve: Boolean = true): Map<String, List<File>> {
        return resolveFiles(filter, resolve).fold(mutableMapOf<String, MutableList<File>>(), { files, resolver ->
            files.getOrPut(resolver.group, { mutableListOf() }).add(resolver.dir); files
        })
    }

    private fun resolveFiles(filter: (String) -> Boolean, resolve: Boolean): List<Resolver> {
        val resolvers = resolvers.filter { filter(it.group) }
        if (resolve) {
            resolvers.onEach(this::resolveFile)
        }

        return resolvers
    }

    private fun resolveFile(resolver: Resolver) {
        resolver.action(resolver)
        if (FileOperations.isDirEmpty(resolver.dir)) {
            throw AemException("Cannot resolve file(s) from group '${resolver.group}' to path: ${resolver.dir}")
        }
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
        resolve(url, {
            download(url, it.dir, { file ->
                SftpFileDownloader(project).download(url, file)
            })
        })
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

            lock.printWriter().use {
                it.print(Formats.toJson(mapOf(
                        "downloaded" to Formats.date()
                )))
            }
        }

        return file
    }

    fun downloadSftpAuth(url: String, username: String? = null, password: String? = null, hostChecking: Boolean? = null) {
        resolve(url, {
            download(url, it.dir, { file ->
                val downloader = SftpFileDownloader(project)

                downloader.username = username ?: project.properties["aem.sftp.username"] as String?
                downloader.password = password ?: project.properties["aem.sftp.password"] as String?
                downloader.hostChecking = hostChecking ?: BooleanUtils.toBoolean(project.properties["aem.sftp.hostChecking"] as String? ?: "false")

                downloader.download(url, file)
            })
        })
    }

    fun downloadSmb(url: String) {
        resolve(url, {
            download(url, it.dir, { file ->
                SmbFileDownloader(project).download(url, file)
            })
        })
    }

    fun downloadSmbAuth(url: String, domain: String? = null, username: String? = null, password: String? = null) {
        resolve(url, {
            download(url, it.dir, { file ->
                val downloader = SmbFileDownloader(project)

                downloader.domain = domain ?: project.properties["aem.smb.domain"] as String?
                downloader.username = username ?: project.properties["aem.smb.username"] as String?
                downloader.password = password ?: project.properties["aem.smb.password"] as String?

                downloader.download(url, file)
            })
        })
    }

    fun downloadHttp(url: String) {
        resolve(url, {
            download(url, it.dir, { file ->
                HttpFileDownloader(project).download(url, file)
            })
        })
    }

    fun downloadHttpAuth(url: String, user: String? = null, password: String? = null, ignoreSSL: Boolean? = null) {
        resolve(arrayOf(url, user, password), {
            download(url, it.dir, { file ->
                val downloader = HttpFileDownloader(project)

                downloader.username = user ?: project.properties["aem.http.username"] as String?
                downloader.password = password ?: project.properties["aem.http.password"] as String?
                downloader.ignoreSSLErrors = ignoreSSL ?: BooleanUtils.toBoolean(project.properties["aem.http.ignoreSSL"] as String? ?: "true")

                downloader.download(url, file)
            })
        })
    }

    fun downloadUrl(url: String) {
        resolve(url, {
            download(url, it.dir, { file ->
                UrlFileDownloader(project).download(url, file)
            })
        })
    }

    fun local(path: String) {
        local(project.file(path))
    }

    fun local(sourceFile: File) {
        resolve(sourceFile.absolutePath, { })
    }

    private fun resolve(hash: Any, resolver: (Resolver) -> Unit) {
        val id = HashCode.fromInt(HashCodeBuilder().append(hash).toHashCode()).toString()
        resolvers += Resolver(id, group, resolver)
    }

    @Synchronized
    fun group(name: String, configurer: Closure<*>) {
        group = name
        ConfigureUtil.configureSelf(configurer, this)
        group = GROUP_DEFAULT
    }

}