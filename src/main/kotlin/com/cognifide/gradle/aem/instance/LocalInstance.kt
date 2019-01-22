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
            "-server", "-Xmx1024m", "-XX:MaxPermSize=256M", "-Djava.awt.headless=true"
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
        get() = File("${aem.config.instanceRoot}/$typeName")

    @get:JsonIgnore
    val jar: File
        get() = File(dir, "aem-quickstart.jar")

    @get:JsonIgnore
    val staticDir: File
        get() = File(dir, "crx-quickstart")

    @get:JsonIgnore
    val license: File
        get() = File(dir, "license.properties")

    @get:JsonIgnore
    val startScript: Script
        get() = binScript("start")

    @get:JsonIgnore
    val pidFile: File
        get() = File("$staticDir/conf/cq.pid")

    @get:JsonIgnore
    val controlPortFile: File
        get() = File("$staticDir/conf/controlport")

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
            Script(File(dir, "$name.bat"), File(staticDir, "bin/$name.bat"), listOf("cmd", "/C"))
        } else {
            Script(File(dir, name), File(staticDir, "bin/$name"), listOf("sh"))
        }
    }

    fun create(options: LocalInstanceOptions) {
        if (created) {
            aem.logger.info(("Instance already created: $this"))
            return
        }

        cleanDir(true)

        aem.logger.info("Creating instance at path '$dir'")

        aem.logger.info("Copying instance files")
        copyFiles(options)

        aem.logger.info("Validating instance files")
        validateFiles()

        aem.logger.info("Extracting AEM static files from JAR")
        extractStaticFiles()

        aem.logger.info("Correcting AEM static files")
        correctStaticFiles()

        aem.logger.info("Creating default instance files")
        FileOperations.copyResourcesFromAemPkg(InstancePlugin.FILES_PATH, dir, true)

        val overridesDir = File(options.overridesPath)

        aem.logger.info("Overriding instance files using: ${overridesDir.absolutePath}")
        if (overridesDir.exists()) {
            FileUtils.copyDirectory(overridesDir, dir)
        }

        val propertiesAll = mapOf("instance" to this) + properties + options.expandProperties

        aem.logger.info("Expanding instance files")
        FileOperations.amendFiles(dir, options.expandFiles) { file, source ->
            aem.props.expand(source, propertiesAll, file.absolutePath)
        }

        aem.logger.info("Creating lock file")
        lock(LOCK_CREATE)

        aem.logger.info("Created instance with success")
    }

    private fun copyFiles(options: LocalInstanceOptions) {
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

    private fun correctStaticFiles() {
        FileOperations.amendFile(binScript("start", OperatingSystem.forName("windows")).bin) { origin ->
            var result = origin

            // Force CMD to be launched in closable window mode. Inject nice title.
            result = result.replace(
                    "start \"CQ\" cmd.exe /K",
                    "start /min \"$this\" cmd.exe /C"
            ) // AEM <= 6.2
            result = result.replace(
                    "start \"CQ\" cmd.exe /C",
                    "start /min \"$this\" cmd.exe /C"
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
        aem.logger.info("Extracting static files from JAR '$jar' to directory '$staticDir'")

        aem.progress(FileOperations.zipCount(jar, JAR_STATIC_FILES_PATH)) {
            FileOperations.zipUnpack(jar, staticDir, JAR_STATIC_FILES_PATH) { increment("Extracting file '$it'") }
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
        aem.logger.info("Destroying at path '${dir.absolutePath}'")

        cleanDir(false)

        aem.logger.info("Destroyed with success")
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

        const val JAR_STATIC_FILES_PATH = "static/"

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