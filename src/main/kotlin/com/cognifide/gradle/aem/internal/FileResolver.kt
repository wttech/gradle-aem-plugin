package com.cognifide.gradle.aem.internal

import com.google.common.hash.HashCode
import groovy.lang.Closure
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.ConfigureUtil
import org.gradle.util.GFileUtils
import java.io.File

class FileResolver(val project: Project, val downloadDir: File) {

    companion object {
        val GROUP_DEFAULT = "default"
    }

    val logger: Logger = project.logger

    data class Resolver(val id: String, val group: String, val callback: (File) -> File)

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

    fun resolveFiles(): List<File> {
        return resolveFiles { true }
    }

    fun resolveFiles(filter: (Resolver) -> Boolean): List<File> {
        return resolvers.filter(filter).map { it.callback(File("$downloadDir/${it.id}")) }
    }

    fun url(url: String) {
        if (SmbFileDownloader.handles(url)) {
            downloadSmbAuth(url)
        } else if (UrlFileDownloader.handles(url)) {
            downloadHttp(url)
        } else {
            local(url)
        }
    }

    private fun downloadFileFor(targetDir: File, sourceUrl: String): File {
        GFileUtils.mkdirs(targetDir)

        return File(targetDir, FilenameUtils.getName(sourceUrl))
    }

    fun downloadSmb(url: String) {
        resolve(url, { dir ->
            val file = downloadFileFor(dir, url)
            val downloader = SmbFileDownloader(project)

            downloader.download(url, file)

            file
        })
    }

    fun downloadSmbAuth(url: String, domain: String? = null, username: String? = null, password: String? = null) {
        resolve(url, { dir ->
            val file = downloadFileFor(dir, url)
            val downloader = SmbFileDownloader(project)

            downloader.domain = domain ?: project.properties["aem.smb.domain"] as String?
            downloader.username = username ?: project.properties["aem.smb.username"] as String?
            downloader.password = password ?: project.properties["aem.smb.password"] as String?

            downloader.download(url, file)

            file
        })
    }

    fun downloadHttp(url: String) {
        resolve(url, { dir ->
            val file = downloadFileFor(dir, url)
            UrlFileDownloader(project).download(url, file)
            file
        })
    }

    fun downloadHttpAuth(url: String, user: String? = null, password: String? = null) {
        resolve(arrayOf(url, user, password), { dir ->
            val file = downloadFileFor(dir, url)
            val downloader = UrlFileDownloader(project)

            downloader.user = user ?: project.properties["aem.http.user"] as String?
            downloader.password = password ?: project.properties["aem.http.password"] as String?

            downloader.download(url, file)

            file
        })
    }

    fun local(path: String) {
        local(project.file(path))
    }

    fun local(sourceFile: File): Unit {
        resolve(sourceFile.absolutePath, { sourceFile })
    }

    fun resolve(hash: Any, resolver: (File) -> File): Unit {
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