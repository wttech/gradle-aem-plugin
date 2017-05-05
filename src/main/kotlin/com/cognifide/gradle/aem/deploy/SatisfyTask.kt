package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
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
    }

    @Internal
    private val providers = mutableListOf<() -> File>()

    @OutputDirectory
    private val downloadDir = File(project.buildDir, "${SatisfyTask.NAME}/$DOWNLOAD_DIR")

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

            val packageFiles = providers.map { it() }

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
        provide { project.file(path) }
    }

    fun local(file: File): Unit {
        provide {
            logger.info("Local package used from path: ${file.absolutePath}'")

            file
        }
    }

    fun provide(provider: () -> File): Unit {
        providers += provider
    }

}