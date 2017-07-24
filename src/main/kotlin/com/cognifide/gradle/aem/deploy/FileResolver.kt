package com.cognifide.gradle.aem.deploy

import groovy.lang.Closure
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.ConfigureUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLConnection
import java.util.*

class FileResolver(val project: Project, val downloadDir: File) {

    companion object {
        val GROUP_DEFAULT = "default"
    }

    val logger: Logger = project.logger

    data class Resolver(val groupName: String, val resolver: () -> File)

    private val resolvers = mutableListOf<Resolver>()

    private var groupName: String = GROUP_DEFAULT

    val configured: Boolean
        get() = resolvers.isNotEmpty()

    fun resolveFiles(): List<File> {
        return resolveFiles { true }
    }

    fun resolveFiles(filter: (Resolver) -> Boolean): List<File> {
        return resolvers.filter(filter).map { it.resolver() }
    }

    fun download(url: String) {
        if (url.startsWith("smb://")) {
            downloadSmb(url)
        } else {
            downloadHttp(url, {})
        }
    }

    fun downloadBasicAuth(url: String, user: String = "admin", password: String = "admin") {
        downloadHttp(url, { conn ->
            logger.info("Downloading with basic authorization support. Used credentials: [user=$user][password=$password]")

            conn.setRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString("$user:$password".toByteArray())}")
        })
    }

    private fun downloadHttp(url: String, configurer: (URLConnection) -> Unit) {
        resolve {
            val file = File(downloadDir, FilenameUtils.getName(url))

            if (file.exists()) {
                logger.info("Reusing previously downloaded file from URL: $url")

                if (FileUtils.sizeOf(file) == 0L) {
                    logger.warn("Corrupted file detected '${file.absolutePath}'. Deleting file from URL: $url")
                    file.delete()
                    download(url, file, configurer)
                }
            } else {
                download(url, file, configurer)
            }

            file
        }
    }

    private fun downloadSmb(url: String) {
        resolve {
            val localFile = File(downloadDir, FilenameUtils.getName(url))

            logger.info("Downloading from Samba URL '$url' to '${localFile.absolutePath}'")
            SmbFileResolver.of(project).download(url, localFile)

            localFile
        }
    }

    private fun download(url: String, file: File, configurer: (URLConnection) -> Unit) {
        logger.info("Downloading file from URL: $url")

        val out = BufferedOutputStream(FileOutputStream(file))
        val connection = URL(url).openConnection()

        configurer(connection)
        try {
            connection.getInputStream().use { input ->
                out.use { fileOut ->
                    input.copyTo(fileOut)
                }
            }
        } catch (e: Exception) {
            throw DeployException("Cannot download file from URL $url or transfer it to path: ${file.absolutePath}", e)
        }

        logger.info("File downloaded into path: ${file.absolutePath}")
    }

    fun local(path: String) {
        local(project.file(path))
    }

    fun local(file: File): Unit {
        resolve {
            logger.info("Local file used from path: ${file.absolutePath}'")

            file
        }
    }

    fun resolve(resolver: () -> File): Unit {
        resolvers += Resolver(groupName, resolver)
    }

    @Synchronized
    fun group(name: String, configurer: Closure<*>) {
        groupName = name
        ConfigureUtil.configureSelf(configurer, this)
        groupName = GROUP_DEFAULT
    }

}