package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import groovy.lang.Closure
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.util.ConfigureUtil
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.URLConnection
import java.util.*

open class SatisfyTask : AbstractTask() {

    companion object {
        val NAME = "aemSatisfy"

        val DOWNLOAD_DIR = "downloadDir"

        val GROUP_DEFAULT = "default"

        val GROUP_FILTER_PROPERTY = "aem.deploy.satisfy.group"

        val GROUP_FILTER_DEFAULT = "*"
    }

    private data class Provider(val groupName: String, val provider: () -> File)

    @Internal
    private val providers = mutableListOf<Provider>()

    @OutputDirectory
    private val downloadDir = File(project.buildDir, "${SatisfyTask.NAME}/$DOWNLOAD_DIR")

    @Internal
    private var groupName: String = GROUP_DEFAULT

    init {
        group = AemPlugin.TASK_GROUP
        description = "Satisfies AEM by uploading & installing dependent packages on instance(s)."

        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
    }

    @TaskAction
    fun satisfy() {
        deploy({ sync ->
            logger.info("Providing packages from local and remote sources.")

            val groupFilter = project.properties.getOrElse(GROUP_FILTER_PROPERTY, { GROUP_FILTER_DEFAULT }) as String
            val packageFiles = providers.filter({
                groupFilter.split(",").any { FilenameUtils.wildcardMatch(it, groupFilter, IOCase.INSENSITIVE) }
            }).map { it.provider() }

            logger.info("Packages provided (${packageFiles.size})")
            logger.info("Satisfying (uploading & installing)")

            packageFiles.onEach { packageFile ->
                installPackage(uploadPackage(packageFile, sync).path, sync)
            }

            filterInstances().onEach { instance ->
                packageFiles.onEach { logger.info("Satisfied: ${it.absolutePath} on: $instance") }
            }
        })
    }

    fun download(url: String) {
        download(url, {})
    }

    fun downloadBasicAuth(url: String, user: String = "admin", password: String = "admin") {
        download(url, { conn ->
            logger.info("Downloading with basic authorization support. Used credentials: [user=$user][password=$password]")

            conn.setRequestProperty("Authorization", "Basic ${Base64.getEncoder().encodeToString("$user:$password".toByteArray())}")
        })
    }

    private fun download(url: String, configurer: (URLConnection) -> Unit) {
        provide {
            val file = File(downloadDir, FilenameUtils.getName(url))

            if (file.exists()) {
                logger.info("Reusing previously downloaded package from URL: $url")

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

    private fun download(url: String, file: File, configurer: (URLConnection) -> Unit) {
        logger.info("Downloading package from URL: $url")

        val out = BufferedOutputStream(FileOutputStream(file))
        val connection = URL(url).openConnection()

        configurer(connection)
        connection.getInputStream().use { input ->
            out.use { fileOut ->
                input.copyTo(fileOut)
            }
        }

        logger.info("Packaged downloaded into path: ${file.absolutePath}")
    }

    fun local(path: String) {
        local(project.file(path))
    }

    fun local(file: File): Unit {
        provide {
            logger.info("Local package used from path: ${file.absolutePath}'")

            file
        }
    }

    fun provide(provider: () -> File): Unit {
        providers += Provider(groupName, provider)
    }

    @Synchronized
    fun group(name: String, configurer: Closure<*>) {
        groupName = name
        ConfigureUtil.configureSelf(configurer, this)
        groupName = GROUP_DEFAULT
    }

}