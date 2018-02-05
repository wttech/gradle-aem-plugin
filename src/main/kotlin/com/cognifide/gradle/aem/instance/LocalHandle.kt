package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.ProgressLogger
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.internal.file.resolver.FileResolver
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class LocalHandle(val project: Project, val sync: InstanceSync) {

    companion object {
        val JAR_STATIC_FILES_PATH = "static/"

        val JAR_NAME_PATTERNS = listOf(
                "*aem-quickstart*.jar",
                "*cq-quickstart*.jar",
                "*quickstart*.jar",
                "*.jar"
        )
    }

    class Script(val wrapper: File, val bin: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(wrapper.absolutePath)

        override fun toString(): String {
            return "Script(commandLine=$commandLine)"
        }
    }

    val instance = sync.instance

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val dir = File("${config.instancesPath}/${instance.name}")

    val jar = File(dir, "aem-quickstart.jar")

    val lock = File(dir, "local-handle.lock")

    val staticDir = File(dir, "crx-quickstart")

    val license = File(dir, "license.properties")

    val startScript: Script
        get() = binScript("start")

    val stopScript: Script
        get() = binScript("stop")

    private fun binScript(name: String, os: OperatingSystem = OperatingSystem.current()): Script {
        return if (os.isWindows) {
            Script(File(dir, "$name.bat"), File(staticDir, "bin/$name.bat"), listOf("cmd", "/C"))
        } else {
            Script(File(dir, name), File(staticDir, "bin/$name"), listOf("sh"))
        }
    }

    fun create(fileResolver: FileResolver) {
        if (lock.exists()) {
            logger.info(("Instance already created"))
            return
        }

        cleanDir(true)

        logger.info("Creating instance at path '${dir.absolutePath}'")

        val resolvedFiles = fileResolver.allFiles()
        logger.info("Copying resolved instance files: $resolvedFiles")
        copyFiles(resolvedFiles)

        logger.info("Validating instance files")
        validateFiles()

        logger.info("Extracting AEM static files from JAR")
        extractStaticFiles()

        logger.info("Correcting AEM static files")
        correctStaticFiles()

        logger.info("Creating default instance files")
        FileOperations.copyResources(InstancePlugin.FILES_PATH, dir, true)

        val filesDir = File(config.instanceFilesPath)

        logger.info("Overriding instance files using: ${filesDir.absolutePath}")
        if (filesDir.exists()) {
            FileUtils.copyDirectory(filesDir, dir)
        }

        logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, config.instanceFilesExpanded, { file, source ->
            PropertyParser(project).expandEnv(source, properties, file.absolutePath)
        })

        logger.info("Creating lock file")
        lock()

        logger.info("Created instance with success")
    }

    private fun copyFiles(resolvedFiles: List<File>) {
        GFileUtils.mkdirs(dir)
        val files = resolvedFiles.map {
            FileUtils.copyFileToDirectory(it, dir)
            File(dir, it.name)
        }
        findJar(files)?.let { FileUtils.moveFile(it, jar) }
    }

    private fun findJar(files: List<File>): File? {
        JAR_NAME_PATTERNS.forEach { pattern ->
            files.asSequence()
                    .filter { Patterns.wildcard(it.name, pattern) }
                    .forEach { return it }
        }

        return null
    }

    private fun validateFiles() {
        if (!jar.exists()) {
            throw AemException("Instance JAR file not found at path: ${jar.absolutePath}. Is instance JAR URL configured?")
        }

        if (!license.exists()) {
            throw AemException("License file not found at path: ${license.absolutePath}. Is instance license URL configured?")
        }
    }

    private fun correctStaticFiles() {
        // Force CMD to be launched in closable window mode. Inject nice title.
        FileOperations.amendFile(binScript("start", OperatingSystem.forName("windows")).bin, {
            it.replace("start \"CQ\" cmd.exe /K", "start /min \"$instance\" cmd.exe /C") // AEM <= 6.2
            it.replace("start \"CQ\" cmd.exe /C", "start /min \"$instance\" cmd.exe /C") // AEM 6.3
        })

        // Ensure that 'logs' directory exists
        GFileUtils.mkdirs(File(staticDir, "logs"))
    }

    private fun extractStaticFiles() {
        val progressLogger = ProgressLogger(project, "Extracting static files from JAR  '${jar.absolutePath}' to directory: $staticDir")
        progressLogger.started()

        var total = 0
        ZipUtil.iterate(jar, { entry ->
            if (entry.name.startsWith(JAR_STATIC_FILES_PATH)) {
                total++
            }
        })

        var processed = 0
        ZipUtil.unpack(jar, staticDir, { name ->
            if (name.startsWith(JAR_STATIC_FILES_PATH)) {
                val fileName = name.substringAfterLast("/")

                progressLogger.progress("Extracting: $fileName [${Formats.percent(processed, total)}]")
                processed++
                name.substring(JAR_STATIC_FILES_PATH.length)
            } else {
                name
            }
        })

        progressLogger.completed()
    }

    private fun cleanDir(create: Boolean) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        if (create) {
            dir.mkdirs()
        }
    }

    fun up() {
        logger.info("Executing start script: $startScript")
        execute(startScript)
    }

    fun down() {
        logger.info("Executing stop script: $stopScript")
        execute(stopScript)
    }

    private fun execute(script: Script) {
        ProcessBuilder(*script.commandLine.toTypedArray())
                .directory(dir)
                .start()
    }

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "instance" to sync.instance,
                    "instancePath" to dir.absolutePath,
                    "handle" to this
            )
        }

    fun destroy() {
        logger.info("Destroying at path '${dir.absolutePath}'")

        cleanDir(false)

        logger.info("Destroyed with success")
    }

    fun lock() {
        val metaJson = Formats.toJson(mapOf("locked" to Formats.date()))
        lock.printWriter().use { it.print(metaJson) }
    }

    override fun toString(): String {
        return "LocalHandle(dir=${dir.absolutePath}, instance=$instance)"
    }

}