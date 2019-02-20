package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.file.FileOperations
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.File
import java.io.Serializable
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.internal.os.OperatingSystem
import org.gradle.util.GFileUtils

class LocalInstance private constructor(aem: AemExtension) : AbstractInstance(aem), Serializable {

    override lateinit var httpUrl: String

    override val user: String = USER

    override lateinit var password: String

    override lateinit var typeName: String

    override lateinit var environment: String

    var debugPort: Int = 5005

    @get:JsonIgnore
    val jvmOptsDefaults: List<String>
        get() = mutableListOf<String>().apply {
            if (debugPort > 0) {
                add("-Xdebug")
                add("-Xrunjdwp:transport=dt_socket,address=$debugPort,server=y,suspend=n")
            }
            if (password != Instance.PASSWORD_DEFAULT) {
                add("-Dadmin.password=$password")
            }
        }

    @get:JsonIgnore
    var jvmOpts: List<String> = listOf(
            "-server", "-Xmx2048m", "-XX:MaxPermSize=512M", "-Djava.awt.headless=true"
    )

    @get:JsonProperty("jvmOpts")
    val jvmOptsString: String
        get() = (jvmOptsDefaults + jvmOpts).joinToString(" ")

    @get:JsonIgnore
    var startOpts: List<String> = listOf()

    @get:JsonProperty("startOpts")
    val startOptsString: String
        get() = startOpts.joinToString(" ")

    @get:JsonIgnore
    val runModesDefault
        get() = listOf(type.name.toLowerCase())

    @get:JsonIgnore
    var runModes: List<String> = listOf(ENVIRONMENT)

    @get:JsonProperty("runModes")
    val runModesString: String
        get() = (runModesDefault + runModes).joinToString(",")

    @get:JsonIgnore
    val dir: File
        get() = File("${aem.config.localInstanceOptions.root}/$typeName")

    @get:JsonIgnore
    val jar: File
        get() = File(dir, "aem-quickstart.jar")

    @get:JsonIgnore
    val quickstartDir: File
        get() = File(dir, "crx-quickstart")

    @get:JsonIgnore
    val license: File
        get() = File(dir, "license.properties")

    @get:JsonIgnore
    val startScript: Script
        get() = binScript("start")

    @get:JsonIgnore
    val pidFile: File
        get() = File("$quickstartDir/conf/cq.pid")

    @get:JsonIgnore
    val controlPortFile: File
        get() = File("$quickstartDir/conf/controlport")

    @get:JsonIgnore
    val running: Boolean
        get() = pidFile.exists() && controlPortFile.exists()

    @get:JsonIgnore
    val stopScript: Script
        get() = binScript("stop")

    @get:JsonIgnore
    val created: Boolean
        get() = locked(LOCK_CREATE)

    @get:JsonIgnore
    val initialized: Boolean
        get() = locked(LOCK_INIT)

    private fun binScript(name: String, os: OperatingSystem = OperatingSystem.current()): Script {
        return if (os.isWindows) {
            Script(File(dir, "$name.bat"), File(quickstartDir, "bin/$name.bat"), listOf("cmd", "/C"))
        } else {
            Script(File(dir, name), File(quickstartDir, "bin/$name"), listOf("sh"))
        }
    }

    private val options: LocalInstanceOptions
        get() = aem.config.localInstanceOptions

    fun create() {
        if (created) {
            aem.logger.info(("Instance already created: $this"))
            return
        }

        aem.logger.info("Creating: $this")

        cleanDir(true)
        copyFiles()
        validateFiles()
        unpackFiles()
        correctFiles()
        customize()
        lock(LOCK_CREATE)

        aem.logger.info("Created: $this")
    }

    private fun copyFiles() {
        GFileUtils.mkdirs(dir)

        options.license?.let { FileUtils.copyFile(options.license, license) }
        options.jar?.let { FileUtils.copyFile(options.jar, jar) }

        options.extraFiles.map { file ->
            FileUtils.copyFileToDirectory(file, dir)
            File(dir, file.name)
        }
    }

    private fun validateFiles() {
        if (!jar.exists()) {
            throw AemException("Instance JAR file not found at path: ${jar.absolutePath}. Is instance JAR URL configured?")
        }

        if (!license.exists()) {
            throw AemException("License file not found at path: ${license.absolutePath}. Is instance license URL configured?")
        }
    }

    private fun correctFiles() {
        FileOperations.amendFile(binScript("start", OperatingSystem.forName("windows")).bin) { origin ->
            var result = origin

            // Force CMD to be launched in closable window mode.
            result = result.replace(
                    "start \"CQ\" cmd.exe /K",
                    "start /min \"CQ\" cmd.exe /C"
            ) // AEM <= 6.2
            result = result.replace(
                    "start \"CQ\" cmd.exe /C",
                    "start /min \"CQ\" cmd.exe /C"
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
        GFileUtils.mkdirs(File(quickstartDir, "logs"))
    }

    private fun unpackFiles() {
        aem.logger.info("Unpacking quickstart from JAR '$jar' to directory '$quickstartDir'")

        aem.progressIndicator {
            message = "Unpacking quickstart JAR: ${jar.name}, size: ${Formats.size(jar)}"
            aem.project.javaexec { spec ->
                spec.workingDir = dir
                spec.main = "-jar"
                spec.args = listOf(jar.name, "-unpack")
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

    fun customize() {
        aem.logger.info("Customizing: $this")

        FileOperations.copyFromAemPkg(InstancePlugin.FILES_PATH, dir, false)

        val overridesDir = File(options.overridesPath)
        if (overridesDir.exists()) {
            FileUtils.copyDirectory(overridesDir, dir)
        }

        val propertiesAll = mapOf("instance" to this) + properties + options.expandProperties

        FileOperations.amendFiles(dir, options.expandFiles) { file, source ->
            aem.props.expand(source, propertiesAll, file.absolutePath)
        }

        FileOperations.amendFile(binScript("start", OperatingSystem.forName("windows")).bin) { origin ->
            var result = origin

            // Update window title
            val previousTitle = StringUtils.substringBetween(origin, "start /min \"", "\" cmd.exe ")
            if (previousTitle != null) {
                result = StringUtils.replace(result,
                        "start /min \"$previousTitle\" cmd.exe ",
                        "start /min \"${this}\" cmd.exe "
                )
            }

            result
        }

        aem.logger.info("Customized: $this")
    }

    fun up() {
        if (!created) {
            aem.logger.warn("Instance not created, so it could not be up: $this")
            return
        }

        customize()

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
            sync.stopFramework()
        } catch (e: InstanceException) {
            // ignore, fallback when script failed
        }
    }

    fun init(callback: LocalInstance.() -> Unit) {
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

    fun destroy() {
        aem.logger.info("Destroying: $this")

        cleanDir(false)

        aem.logger.info("Destroyed: $this")
    }

    private fun lockFile(name: String): File = File(dir, "$name.lock")

    fun lock(name: String) {
        val metaJson = Formats.toJson(mapOf("locked" to Formats.date()))
        lockFile(name).printWriter().use { it.print(metaJson) }
    }

    fun locked(name: String): Boolean = lockFile(name).exists()

    override fun toString(): String {
        return "LocalInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', typeName='$typeName', debugPort=$debugPort, dir=$dir)"
    }

    class Script(val wrapper: File, val bin: File, val command: List<String>) {
        val commandLine: List<String>
            get() = command + listOf(wrapper.absolutePath)

        override fun toString(): String {
            return "Script(commandLine=$commandLine)"
        }
    }

    companion object {

        const val ENVIRONMENT = "local"

        const val USER = "admin"

        const val LOCK_CREATE = "create"

        const val LOCK_INIT = "init"

        fun create(aem: AemExtension, httpUrl: String, configurer: LocalInstance.() -> Unit): LocalInstance {
            return LocalInstance(aem).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)
                if (instanceUrl.user != LocalInstance.USER) {
                    throw InstanceException("User '${instanceUrl.user}' (other than 'admin') is not allowed while using local instance(s).")
                }

                this.httpUrl = instanceUrl.httpUrl
                this.password = instanceUrl.password
                this.typeName = instanceUrl.typeName
                this.debugPort = instanceUrl.debugPort
                this.environment = ENVIRONMENT

                this.apply(configurer)
            }
        }

        fun create(aem: AemExtension, httpUrl: String): LocalInstance {
            return create(aem, httpUrl) {}
        }
    }
}