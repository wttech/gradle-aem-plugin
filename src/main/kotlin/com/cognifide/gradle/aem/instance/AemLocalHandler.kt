package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.internal.FileOperations
import com.cognifide.gradle.aem.internal.PropertyParser
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.Serializable

/**
 * TODO accept only AemLocalInstance here
 */
class AemLocalHandler(val project: Project, val base: AemInstance) {

    companion object {
        val JAR_STATIC_FILES_PATH = "static/"

        val QUICKSTART_BIN = "crx-quickstart/bin"
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
            return if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                Script(File(dir, "$QUICKSTART_BIN/start.bat"), listOf("cmd", "/c"))
            } else {
                Script(File(dir, "$QUICKSTART_BIN/bin/start"), listOf("sh"))
            }
        }

    val stopScript: Script
        get() {
            return if (Os.isFamily(Os.FAMILY_WINDOWS)) {
                Script(File(dir, "$QUICKSTART_BIN/stop.bat"), listOf("cmd", "/c", "start"))
            } else {
                Script(File(dir, "$QUICKSTART_BIN/stop"), listOf("sh"))
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

        val filesDir = File(config.instanceFilesPath)

        logger.info("Overriding instance files using dir: ${filesDir.absolutePath}")
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
        Runtime.getRuntime().exec(startScript.commandLine.toTypedArray())
    }

    fun down() {
        Runtime.getRuntime().exec(stopScript.commandLine.toTypedArray())
    }

    val properties: Map<String, Serializable>
        get() {
            return mapOf(
                    "instance" to base,
                    "jar" to jar,
                    "license" to license
            )
        }

    fun destroy() {
        logger.info("Destroying instance at path '${dir.absolutePath}'")

        cleanDir(false)

        logger.info("Destroyed instance with success")
    }

    override fun toString(): String {
        return "AemLocalHandler(dir=${dir.absolutePath})"
    }

}