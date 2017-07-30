package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemInstancePlugin
import com.cognifide.gradle.aem.internal.FileOperations
import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

/**
 * TODO accept only AemLocalInstance here
 */
class AemLocalHandler(val project: Project, val base: AemInstance) {

    companion object {
        val JAR_STATIC_FILES_PATH = "static/"
    }

    class Script(val script: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(script.absolutePath)
    }

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val dir = File("${config.instancesPath}/${base.name}")

    val jar: File by lazy {
        FileOperations.find(dir, listOf("cq-quickstart*.jar")) ?: File(dir, "cq-quickstart.jar")
    }

    val staticDir = File(dir, "crx-quickstart")

    val license = File(dir, "license.properties")

    val startScript: Script
        get() {
            return if (OperatingSystem.current().isWindows) {
                Script(File(dir, "start.bat"), listOf("cmd", "/C"))
            } else {
                Script(File(dir, "start.sh"), listOf("sh"))
            }
        }

    val stopScript: Script
        get() {
            return if (OperatingSystem.current().isWindows) {
                Script(File(dir, "stop.bat"), listOf("cmd", "/C"))
            } else {
                Script(File(dir, "stop.sh"), listOf("sh"))
            }
        }

    fun create(files: List<File>) {
        cleanDir(true)

        logger.info("Creating instance at path '${dir.absolutePath}'")

        logger.info("Copying resolved instance files: ${files.map { it.absolutePath }}")
        GFileUtils.mkdirs(dir)
        files.forEach { FileUtils.copyFileToDirectory(it, dir) }

        logger.info("Extracting static files from JAR")
        extract()

        logger.info("Creating default instance files")
        FileOperations.copyResources(AemInstancePlugin.FILES_PATH, dir, true)

        val filesDir = File(config.instanceFilesPath)

        logger.info("Overriding instance files using project dir: ${filesDir.absolutePath}")
        if (filesDir.exists()) {
            FileUtils.copyDirectory(filesDir, dir)
        }

        logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, config.instanceFilesExpanded, { source ->
            PropertyParser(project).expand(source, properties)
        })

        logger.info("Created instance with success")
    }

    // TODO maybe introduce progress bar here somehow
    fun extract() {
        ZipUtil.unpack(jar, staticDir) { name ->
            if (name.startsWith(JAR_STATIC_FILES_PATH)) {
                name.substring(JAR_STATIC_FILES_PATH.length)
            } else {
                name
            }
        }
    }

    private fun cleanDir(create: Boolean) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        if (create) {
            dir.mkdirs()
        }
    }

    fun validate() {
        if (!jar.exists()) {
            throw AemException("Instance JAR file not found at path: ${jar.absolutePath}")
        }

        if (!license.exists()) {
            throw AemException("License file not found at path: ${license.absolutePath}")
        }
    }

    fun up() {
        ProcessBuilder(*startScript.commandLine.toTypedArray())
                .directory(dir)
                .start()
    }

    fun down() {
        ProcessBuilder(*stopScript.commandLine.toTypedArray())
                .directory(dir)
                .start()
    }

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "instance" to base,
                    "instancePath" to dir.absolutePath,
                    "handler" to this
            )
        }

    fun destroy() {
        logger.info("Destroying instance at path '${dir.absolutePath}'")

        cleanDir(false)

        logger.info("Destroyed instance with success")
    }

    override fun toString(): String {
        return "AemLocalHandler(dir=${dir.absolutePath}, base=$base)"
    }

}