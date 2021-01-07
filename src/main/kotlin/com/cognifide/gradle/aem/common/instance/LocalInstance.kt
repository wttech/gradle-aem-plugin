package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemVersion
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.local.Script
import com.cognifide.gradle.aem.common.instance.local.Status
import com.cognifide.gradle.aem.common.instance.oak.OakRun
import com.cognifide.gradle.aem.common.instance.service.osgi.Bundle
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.zip.ZipFile
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import org.gradle.internal.os.OperatingSystem
import java.io.File
import java.io.FileFilter

@Suppress("TooManyFunctions")
class LocalInstance private constructor(aem: AemExtension) : Instance(aem) {

    override var user: String = USER

    var debugPort: Int = 5005

    var debugAddress: String = ""

    var openPath: String = "/"

    val httpOpenUrl get() = when (openPath) {
        "/" -> httpUrl
        else -> "${httpUrl}$openPath"
    }

    private val debugSocketAddress: String get() = when (debugAddress) {
        "*" -> "0.0.0.0:$debugPort"
        "" -> "$debugPort"
        else -> "$debugAddress:$debugPort"
    }

    @get:JsonIgnore
    val jvmOptsDefaults: List<String> get() = mutableListOf<String>().apply {
        if (debugPort in 1..65535) {
            add(jvmDebugOpt)
        }
        if (password != PASSWORD_DEFAULT) {
            add("-Dadmin.password=$password")
        }
    }

    @get:JsonIgnore
    private val jvmDebugOpt: String get() = when {
        SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_9) ->
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$debugSocketAddress"
        else ->
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$debugPort"
    }

    @get:JsonIgnore
    var jvmOpts: List<String> = listOf(
            "-server", "-Xmx2048m", "-XX:MaxPermSize=512M", "-Djava.awt.headless=true"
    )

    @get:JsonProperty("jvmOpts")
    val jvmOptsString: String get() = (jvmOptsDefaults + jvmOpts).joinToString(" ")

    @get:JsonIgnore
    val javaExecutablePath: String get() = localManager.javaExecutablePath

    @get:JsonIgnore
    var startOpts: List<String> = listOf()

    @get:JsonProperty("startOpts")
    val startOptsString: String get() = startOpts.joinToString(" ")

    @get:JsonIgnore
    val runModesDefault get() = listOf(type.name.toLowerCase())

    @get:JsonIgnore
    var runModes: List<String> = listOf(ENVIRONMENT)

    @get:JsonProperty("runModes")
    val runModesString: String get() = (runModesDefault + runModes).joinToString(",")

    @get:JsonIgnore
    val dir: File get() = aem.localInstanceManager.instanceDir.get().asFile.resolve(id)

    @get:JsonIgnore
    val controlDir: File get() = dir.resolve("control")

    @get:JsonIgnore
    val overridesDirs: List<File> get() = localManager.overrideDir.get().asFile.run { listOf(resolve("common"), resolve(id)) }

    @get:JsonIgnore
    val jar: File? get() = quickstartDir.resolve("app").takeIf { it.exists() }?.listFiles(FileFilter { it.extension == "jar" })?.firstOrNull()

    @get:JsonIgnore
    val license: File get() = dir.resolve("license.properties")

    @get:JsonIgnore
    val quickstartDir: File get() = dir.resolve("crx-quickstart")

    @get:JsonIgnore
    val bundlesDir: File get() = quickstartDir.resolve("launchpad/felix")

    fun bundleDir(bundle: Bundle) = bundleDir(bundle.id.toInt())

    fun bundleDir(no: Int) = bundlesDir.resolve("bundle$no")

    @get:JsonIgnore
    val pidFile: File get() = quickstartDir.resolve("conf/cq.pid")

    @get:JsonIgnore
    val pid: Int get() = pidFile.takeIf { it.exists() }?.readText()
            ?.trim()?.ifBlank { null }?.toInt() ?: 0

    @get:JsonIgnore
    val logsDir: File get() = quickstartDir.resolve("logs")

    @get:JsonIgnore
    val stdoutLog: File get() = logsDir.resolve("stdout.log")

    @get:JsonIgnore
    val errorLog: File get() = logsDir.resolve("error.log")

    @get:JsonIgnore
    val requestLog: File get() = logsDir.resolve("request.log")

    override val version: AemVersion
        get() {
            val remoteVersion = super.version
            if (remoteVersion != AemVersion.UNKNOWN) {
                return remoteVersion
            }
            val jarVersion = readVersionFromJar()
            if (jarVersion != AemVersion.UNKNOWN) {
                return jarVersion
            }
            return AemVersion.UNKNOWN
        }

    private fun readVersionFromJar() = readVersionFromExtractedJar() ?: readVersionFromQuickstartJar() ?: AemVersion.UNKNOWN

    private fun readVersionFromExtractedJar() = jar?.name?.let { readVersionFromJarFileName(it) }

    private fun readVersionFromQuickstartJar() = quickstartJar
            .let { ZipFile(it).listDir("static/app") }
            .map { it.substringAfterLast("/") }
            .firstOrNull { it.startsWith("cq-quickstart-") && it.endsWith(".jar") }
            ?.let { readVersionFromJarFileName(it) }

    private fun readVersionFromJarFileName(fileName: String) = fileName
            .removePrefix("cq-quickstart-").removePrefix("cloudready-")
            .substringBefore("-").let { AemVersion(it) }

    private val startScript: Script get() = script("start")

    internal fun executeStartScript() {
        try {
            startScript.executeVerbosely { withTimeoutMillis(localManager.startTimeout.get()) }
        } catch (e: LocalInstanceException) {
            throw LocalInstanceException("Instance start script failed! Check resources like disk free space, open HTTP ports etc.", e)
        }
    }

    private val stopScript: Script get() = script("stop")

    internal fun executeStopScript() {
        val pidOrigin = pid
        try {
            stopScript.executeVerbosely { withTimeoutMillis(localManager.stopTimeout.get()) }
        } catch (e: LocalInstanceException) {
            throw LocalInstanceException("Instance stop script failed! Consider killing process manually using PID: $pidOrigin.", e)
        }
    }

    private val statusScript: Script get() = script("status")

    @get:JsonIgnore
    val touched: Boolean get() = dir.exists()

    @get:JsonIgnore
    val created: Boolean get() = locked(LOCK_CREATE)

    @get:JsonIgnore
    val initialized: Boolean get() = locked(LOCK_INIT)

    @get:JsonIgnore
    val installDir get() = quickstartDir.resolve("install")

    @get:JsonIgnore
    val oakRun get() = OakRun(aem, this)

    private fun script(name: String, os: OperatingSystem = OperatingSystem.current()) = if (os.isWindows) {
        Script(this, listOf("cmd", "/C"), controlDir.resolve("$name.bat"), quickstartDir.resolve("bin/$name.bat"))
    } else {
        Script(this, listOf("sh"), controlDir.resolve("$name.sh"), quickstartDir.resolve("bin/$name"))
    }

    private val quickstartJar get() = localManager.quickstart.jar?.takeIf { it.exists() }
            ?: throw LocalInstanceException("Instance JAR file not found! Is instance JAR URL configured?")

    private val quickstartLicense get() = localManager.quickstart.license?.takeIf { it.exists() }
            ?: throw LocalInstanceException("Instance license file not found! Is instance license URL configured?")

    @get:JsonIgnore
    val localManager: LocalInstanceManager get() = aem.localInstanceManager

    fun create() = localManager.create(this)

    internal fun prepare() {
        cleanDir(true)
        unpackFiles()
        correctFiles()
        customize()
        lock(LOCK_CREATE)
    }

    private fun correctFiles() {
        FileOperations.amendFile(script("start", OperatingSystem.forName("windows")).bin) { origin ->
            var result = origin

            // Update 'timeout' to 'ping' as of it does not work when called from process without GUI
            result = result.replace(
                    "timeout /T 1 /NOBREAK >nul",
                    "ping 127.0.0.1 -n 3 > nul"
            )

            // Force AEM to be launched in background
            result = result.replace(
                    "start \"CQ\" cmd.exe /K java %CQ_JVM_OPTS% -jar %CurrDirName%\\%CQ_JARFILE% %START_OPTS%",
                    "cbp.exe cmd.exe /C \"java %CQ_JVM_OPTS% -jar %CurrDirName%\\%CQ_JARFILE% %START_OPTS% 1> %CurrDirName%\\logs\\stdout.log 2>&1\""
            ) // AEM <= 6.2
            result = result.replace(
                    "start \"CQ\" cmd.exe /C java %CQ_JVM_OPTS% -jar %CurrDirName%\\%CQ_JARFILE% %START_OPTS%",
                    "cbp.exe cmd.exe /C \"java %CQ_JVM_OPTS% -jar %CurrDirName%\\%CQ_JARFILE% %START_OPTS% 1> %CurrDirName%\\logs\\stdout.log 2>&1\""
            ) // AEM 6.3

            // Introduce missing CQ_START_OPTS injectable by parent script.
            result = result.replace(
                    "set START_OPTS=start -c %CurrDirName% -i launchpad",
                    "set START_OPTS=start -c %CurrDirName% -i launchpad %CQ_START_OPTS%"
            )

            result
        }

        FileOperations.amendFile(script("start", OperatingSystem.forName("unix")).bin) { origin ->
            var result = origin

            // Introduce missing CQ_START_OPTS injectable by parent script.
            result = result.replace(
                    "START_OPTS=\"start -c ${'$'}{CURR_DIR} -i launchpad\"",
                    "START_OPTS=\"start -c ${'$'}{CURR_DIR} -i launchpad ${'$'}{CQ_START_OPTS}\""
            )

            result
        }

        // Use java executable path explicitly to make instance working even when running from non-interactive shells (e.g as systemd service).
        aem.project.fileTree(dir)
                .matching { it.include(localManager.executableFiles.get()) }
                .forEach { file ->
                    FileOperations.amendFile(file) {
                        it.replace("java ", when (file.extension) {
                            "bat" -> "%JAVA_EXECUTABLE% "
                            else -> "\$JAVA_EXECUTABLE "
                        })
                    }
                }

        // Ensure that 'logs' directory exists
        logsDir.mkdirs()
    }

    private fun unpackFiles() {
        logger.info("Unpacking quickstart from JAR '$quickstartJar' to directory '$quickstartDir'")
        common.progressIndicator {
            message = "Unpacking quickstart JAR: ${quickstartJar.name}, size: ${Formats.fileSize(quickstartJar)}"
            aem.project.javaexec { spec ->
                spec.executable(localManager.javaExecutablePath)
                spec.workingDir = dir
                spec.main = "-jar"
                spec.args = listOf(quickstartJar.absolutePath, "-unpack")
            }
        }

        logger.info("Copying quickstart license from '$quickstartLicense' to '$license'")
        FileUtils.copyFile(quickstartLicense, license)
    }

    internal fun delete() = cleanDir(create = false)

    private fun cleanDir(create: Boolean) {
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        if (create) {
            dir.mkdirs()
        }
    }

    internal fun customize() {
        aem.assetManager.copyDir(FILES_PATH, dir)
        copyOverrideFiles()
        expandFiles()
        copyInstallFiles()
        makeFilesExecutable()
        resetPassword()
    }
    private fun copyOverrideFiles() {
        overridesDirs.filter { it.exists() }.forEach {
            FileUtils.copyDirectory(it, dir)
        }
    }

    private fun expandFiles() {
        val propertiesAll = mapOf(
                "instance" to this,
                "service" to localManager.serviceComposer
        ) + properties + localManager.expandProperties.get()

        aem.project.fileTree(dir)
                .matching { it.include(localManager.expandFiles.get()) }
                .forEach { file ->
                    FileOperations.amendFile(file) { content ->
                        aem.prop.expand(content, propertiesAll, file.absolutePath)
                    }
                }
    }

    private fun copyInstallFiles() {
        val installFiles = localManager.install.files
        if (installFiles.isNotEmpty()) {
            installDir.mkdirs()
            installFiles.forEach { source ->
                val target = installDir.resolve(source.name)
                if (!target.exists()) {
                    logger.info("Copying quickstart install file from '$source' to '$target'")
                    FileUtils.copyFileToDirectory(source, installDir)
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun makeFilesExecutable() {
        if (OperatingSystem.current().isWindows) {
            return
        }

        aem.project.fileTree(dir)
            .matching { it.include(localManager.executableFiles.get()).exclude("**/*.bat") }
            .forEach { file ->
                try {
                    FileOperations.makeExecutable(file)
                } catch (e: Exception) {
                    logger.info("Cannot make file '$file' executable!", e)
                }
            }
    }

    fun up() = localManager.up(this)

    fun down() = localManager.down(this)

    fun open() = localManager.open(this)

    fun kill() = localManager.kill(this)

    @get:JsonIgnore
    val status: Status get() = checkStatus()

    fun checkStatus(): Status {
        var result = Status.UNRECOGNIZED

        if (created) {
            try {
                val exitValue = statusScript.executeQuietly { withTimeoutMillis(localManager.statusTimeout.get()) }.exitValue
                result = Status.byExitValue(exitValue).also { status ->
                    logger.debug("Instance status of $this is $status")
                }
            } catch (e: LocalInstanceException) {
                logger.debug("Instance status checking error: $this", e)
                logger.info("Instance status of $this is not available")
            }
        }

        return result
    }

    @get:JsonIgnore
    val running: Boolean get() = created && checkStatus().running

    @get:JsonIgnore
    val runnable: Boolean get() = created && checkStatus().runnable

    @get:JsonIgnore
    val runningDir get() = aem.project.file(runningPath)

    @get:JsonIgnore
    val runningOther get() = available && (dir != runningDir)

    internal fun init(callback: LocalInstance.() -> Unit) {
        apply(callback)
        lock(LOCK_INIT)
    }

    fun destroy() = localManager.destroy(this)

    private fun lockFile(name: String) = dir.resolve("$name.lock")

    private fun lock(name: String) = FileOperations.lock(lockFile(name))

    private fun locked(name: String): Boolean = lockFile(name).exists()

    fun resetPassword() {
        if (running) {
            throw LocalInstanceException("Instance is running so resetting password on $this is not possible!")
        }
        if (initialized) {
            oakRun.resetPassword(user, password)
        } else {
            logger.debug("Instance is not initialized so resetting password is not needed on $this")
        }
    }

    override fun toString() = "LocalInstance(name='$name', httpUrl='$httpUrl')"

    companion object {

        const val FILES_PATH = "localInstance/defaults"

        const val SERVICE_PATH = "localInstance/service"

        const val ENVIRONMENT = "local"

        const val USER = "admin"

        const val LOCK_CREATE = "create"

        const val LOCK_INIT = "init"

        fun create(aem: AemExtension, httpUrl: String, configurer: LocalInstance.() -> Unit = {}): LocalInstance {
            return LocalInstance(aem).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)
                if (instanceUrl.user != USER) {
                    throw LocalInstanceException("User '${instanceUrl.user}' (other than 'admin') is not allowed while using local instance(s).")
                }

                this.httpUrl = instanceUrl.httpUrl
                this.password = instanceUrl.password
                this.id = instanceUrl.id
                this.debugPort = instanceUrl.debugPort
                this.env = instanceUrl.env

                configurer()
                validate()
            }
        }
    }
}
