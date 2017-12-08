package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemInstancePlugin
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils
import org.gradle.util.VersionNumber
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.FileInputStream
import java.util.*

class LocalHandle(val project: Project, val sync: InstanceSync) {

    companion object {
        val JAR_STATIC_FILES_PATH = "static/"

        val INTERACTIVE_VERSION = VersionNumber.parse("6.3.0")
    }

    class Script(val wrapper: File, val bin: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(wrapper.absolutePath)
    }

    val propertyParser = PropertyParser(project)

    val logger: Logger = project.logger

    val config = AemConfig.of(project)

    val instance = sync.instance

    val dir = File("${config.instancesPath}/${instance.name}")

    val jar: File by lazy {
        FileOperations.find(dir, listOf("cq-quickstart*.jar")) ?: File(dir, "cq-quickstart.jar")
    }

    val lock = File(dir, "local-handle.lock")

    val staticDir = File(dir, "crx-quickstart")

    val license = File(dir, "license.properties")

    val licenseProps : Properties by lazy {
        val result = Properties()
        result.load(FileInputStream(license))

        result
    }

    val version : VersionNumber by lazy {
        val value = licenseProps.getProperty("license.product.version", "6.3.0")
        VersionNumber.parse(value)
    }

    val defaultFilesPath by lazy {
        AemInstancePlugin.FILES_PATH + "/" + if (version >= INTERACTIVE_VERSION) {
            "interactive"
        } else {
            "nointeractive"
        }
    }

    val startScript: Script
        get() = binScript("start")

    val stopScript: Script
        get() = binScript("stop")

    private fun binScript(name: String): Script {
        return if (OperatingSystem.current().isWindows) {
            Script(File(dir, "$name.bat"), File(staticDir, "bin/$name.bat"), listOf("cmd", "/C"))
        } else {
            Script(File(dir, "$name.sh"), File(staticDir, "bin/$name.sh"), listOf("sh"))
        }
    }

    fun create(files: List<File>) {
        cleanDir(true)

        logger.info("Creating instance at path '${dir.absolutePath}'")

        logger.info("Copying resolved instance files: ${files.map { it.absolutePath }}")
        GFileUtils.mkdirs(dir)
        files.forEach { FileUtils.copyFileToDirectory(it, dir) }

        logger.info("Validating instance files")
        validateFiles()

        logger.info("Extracting AEM static files from JAR")
        extractStaticFiles()

        logger.info("Correcting AEM static files")
        correctStaticFiles()

        logger.info("Creating default instance files")
        FileOperations.copyResources(defaultFilesPath, dir, true)

        val filesDir = File(config.instanceFilesPath)

        logger.info("Overriding instance files using: ${filesDir.absolutePath}")
        if (filesDir.exists()) {
            FileUtils.copyDirectory(filesDir, dir)
        }

        logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, config.instanceFilesExpanded, { file, source ->
            propertyParser.expand(source, properties, file.absolutePath)
        })

        logger.info("Creating lock file")
        lock()

        logger.info("Created instance with success")
    }

    fun validateFiles() {
        if (!jar.exists()) {
            throw AemException("Instance JAR file not found at path: ${jar.absolutePath}. Is instance JAR URL configured?")
        }

        if (!license.exists()) {
            throw AemException("License file not found at path: ${license.absolutePath}. Is instance license URL configured?" )
        }
    }

    // TODO This is dirty, but needed to have nice defaults automatically configured.
    private fun correctStaticFiles() {
        if (OperatingSystem.current().isWindows) {
            FileOperations.amendFile(startScript.bin, {
                // Force CMD to be launched in closable window mode. Inject nice title.
                it.replace("start \"CQ\" cmd.exe /K", "start /min \"$instance\" cmd.exe /C") // AEM <= 6.2
                it.replace("start \"CQ\" cmd.exe /C", "start /min \"$instance\" cmd.exe /C") // AEM 6.3

                // Make START_OPTS be extendable by parent script.
                it.replace("set START_OPTS=start -c %CurrDirName% -i launchpad", "if not defined START_OPTS set START_OPTS=start -c %CurrDirName% -i launchpad")
            })
        } else {
            FileOperations.amendFile(startScript.bin, {
                // Make START_OPTS be extendable by parent script.
                it.replace("START_OPTS=\"start -c ${'$'}{CURR_DIR} -i launchpad\"", """
                    if [ -z "${'$'}START_OPTS" ]; then
	                    START_OPTS="start -c ${'$'}{CURR_DIR} -i launchpad"
                    fi
                """.trimIndent())
            })
        }

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
        execute(startScript)
    }

    fun down() {
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
        logger.info("Destroying instance at path '${dir.absolutePath}'")

        cleanDir(false)

        logger.info("Destroyed instance with success")
    }

    fun lock() {
        val metaJson = Formats.toJson(mapOf("locked" to Formats.date()))
        lock.printWriter().use { it.print(metaJson) }
    }

    override fun toString(): String {
        return "LocalHandle(dir=${dir.absolutePath}, instance=$instance)"
    }

}