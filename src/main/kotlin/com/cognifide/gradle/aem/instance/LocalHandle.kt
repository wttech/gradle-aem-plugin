package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.FileOperations
import java.io.File
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils
import org.zeroturnaround.zip.ZipUtil

class LocalHandle(val project: Project, val instance: LocalInstance) {

    val aem = AemExtension.of(project)

    val dir = File("${aem.config.instanceRoot}/${instance.typeName}")

    val jar = File(dir, "aem-quickstart.jar")

    val staticDir = File(dir, "crx-quickstart")

    val license = File(dir, "license.properties")

    val startScript: Script
        get() = binScript("start")

    val pidFile: File
        get() = File("$staticDir/conf/cq.pid")

    val controlPortFile: File
        get() = File("$staticDir/conf/controlport")

    val running: Boolean
        get() = pidFile.exists() && controlPortFile.exists()

    val stopScript: Script
        get() = binScript("stop")

    private fun binScript(name: String, os: OperatingSystem = OperatingSystem.current()): Script {
        return if (os.isWindows) {
            Script(File(dir, "$name.bat"), File(staticDir, "bin/$name.bat"), listOf("cmd", "/C"))
        } else {
            Script(File(dir, name), File(staticDir, "bin/$name"), listOf("sh"))
        }
    }

    fun create(options: LocalHandleOptions, instanceFiles: List<File>) {
        if (created) {
            aem.logger.info(("Instance already created: $this"))
            return
        }

        cleanDir(true)

        aem.logger.info("Creating instance at path '${dir.absolutePath}'")

        aem.logger.info("Copying resolved instance files: $instanceFiles")
        copyFiles(instanceFiles)

        aem.logger.info("Validating instance files")
        validateFiles()

        aem.logger.info("Extracting AEM static files from JAR")
        extractStaticFiles()

        aem.logger.info("Correcting AEM static files")
        correctStaticFiles()

        aem.logger.info("Creating default instance files")
        FileOperations.copyResources(InstancePlugin.FILES_PATH, dir, true)

        val overridesDir = File(options.overridesPath)

        aem.logger.info("Overriding instance files using: ${overridesDir.absolutePath}")
        if (overridesDir.exists()) {
            FileUtils.copyDirectory(overridesDir, dir)
        }

        val propertiesAll = this.properties + options.expandProperties

        aem.logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, options.expandFiles) { file, source ->
            aem.props.expand(source, propertiesAll, file.absolutePath)
        }

        aem.logger.info("Creating lock file")
        lock(LOCK_CREATE)

        aem.logger.info("Created instance with success")
    }

    private fun copyFiles(resolvedFiles: List<File>) {
        GFileUtils.mkdirs(dir)
        val files = resolvedFiles.map { file ->
            FileUtils.copyFileToDirectory(file, dir)
            File(dir, file.name)
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
        FileOperations.amendFile(binScript("start", OperatingSystem.forName("windows")).bin) { origin ->
            var result = origin

            // Force CMD to be launched in closable window mode. Inject nice title.
            result = result.replace(
                    "start \"CQ\" cmd.exe /K",
                    "start /min \"$instance\" cmd.exe /C"
            ) // AEM <= 6.2
            result = result.replace(
                    "start \"CQ\" cmd.exe /C",
                    "start /min \"$instance\" cmd.exe /C"
            ) // AEM 6.3

            // Introduce missing CQ_START_OPTS injectable by parent script.
            result = result.replace(
                    "set START_OPTS=start -c %CurrDirName% -i launchpad",
                    "set START_OPTS=start -c %CurrDirName% -i launchpad %CQ_START_OPTS%"
            )

            result
        }

        FileOperations.amendFile(binScript("start", OperatingSystem.forName("unix")).bin) { origin ->
            var result = origin

            // Introduce missing CQ_START_OPTS injectable by parent script.
            result = result.replace(
                    "START_OPTS=\"start -c ${'$'}{CURR_DIR} -i launchpad\"",
                    "START_OPTS=\"start -c ${'$'}{CURR_DIR} -i launchpad ${'$'}{CQ_START_OPTS}\""
            )

            result
        }

        // Ensure that 'logs' directory exists
        GFileUtils.mkdirs(File(staticDir, "logs"))
    }

    private fun extractStaticFiles() {
        aem.logger.info("Extracting static files from JAR '${jar.absolutePath}' to directory: $staticDir")

        aem.progress {
            ZipUtil.iterate(jar) { entry ->
                if (entry.name.startsWith(JAR_STATIC_FILES_PATH)) {
                    total++
                }
            }

            ZipUtil.unpack(jar, staticDir) { name ->
                if (name.startsWith(JAR_STATIC_FILES_PATH)) {
                    val fileName = name.substringAfterLast("/")
                    increment("Extracting file '$fileName'")
                    name.substring(JAR_STATIC_FILES_PATH.length)
                } else {
                    name
                }
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

    fun up() {
        if (!created) {
            aem.logger.warn("Instance not created, so it could not be up: $this")
            return
        }

        aem.logger.info("Executing start script: $startScript")
        execute(startScript)
    }

    fun down() {
        if (!created) {
            aem.logger.warn("Instance not created, so it could not be down: $this")
            return
        }

        aem.logger.info("Executing stop script: $stopScript")
        execute(stopScript)

        try {
            instance.sync.stopFramework()
        } catch (e: InstanceException) {
            // ignore, fallback when script failed
        }
    }

    fun init(callback: LocalHandle.() -> Unit) {
        if (initialized) {
            aem.logger.debug("Instance already initialized: $this")
            return
        }

        aem.logger.info("Initializing running instance")
        callback(this)
        lock(LOCK_INIT)
    }

    private fun execute(script: Script) {
        ProcessBuilder(script.commandLine)
                .directory(dir)
                .start()
    }

    val properties: Map<String, Any>
        get() {
            return mapOf(
                    "instance" to instance,
                    "handle" to this
            )
        }

    fun destroy() {
        aem.logger.info("Destroying at path '${dir.absolutePath}'")

        cleanDir(false)

        aem.logger.info("Destroyed with success")
    }

    val created: Boolean
        get() = locked(LOCK_CREATE)

    val initialized: Boolean
        get() = locked(LOCK_INIT)

    private fun lockFile(name: String): File = File(dir, "$name.lock")

    fun lock(name: String) {
        val metaJson = Formats.toJson(mapOf("locked" to Formats.date()))
        lockFile(name).printWriter().use { it.print(metaJson) }
    }

    fun locked(name: String): Boolean = lockFile(name).exists()

    override fun toString(): String {
        return "LocalHandle(dir=${dir.absolutePath}, instance=$instance)"
    }

    companion object {
        const val JAR_STATIC_FILES_PATH = "static/"

        val JAR_NAME_PATTERNS = listOf(
                "*aem-quickstart*.jar",
                "*cq-quickstart*.jar",
                "*quickstart*.jar",
                "*.jar"
        )

        const val LOCK_CREATE = "create"

        const val LOCK_INIT = "init"
    }

    class Script(val wrapper: File, val bin: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(wrapper.absolutePath)

        override fun toString(): String {
            return "Script(commandLine=$commandLine)"
        }
    }
}

val List<LocalHandle>.names: String?
    get() = if (isNotEmpty()) joinToString(", ") { it.instance.name } else "none"