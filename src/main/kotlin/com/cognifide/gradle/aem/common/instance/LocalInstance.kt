package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemVersion
import com.cognifide.gradle.aem.common.CommonOptions
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.local.JavaAgentResolver
import com.cognifide.gradle.aem.common.instance.local.Script
import com.cognifide.gradle.aem.common.instance.local.Status
import com.cognifide.gradle.aem.common.instance.oak.OakRun
import com.cognifide.gradle.aem.common.instance.service.osgi.Bundle
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.using
import org.apache.commons.io.FileUtils
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File
import java.io.FileFilter

@Suppress("TooManyFunctions")
class LocalInstance(aem: AemExtension, name: String) : Instance(aem, name) {

    val debugPort = common.obj.int {
        convention(aem.obj.provider { httpUrlDetails.debugPort })
        prop.int("debugPort")?.let { set(it) }
    }

    val debugAddress = common.obj.string {
        prop.string("debugAddress")?.let { set(it) }
    }

    val openPath = common.obj.string {
        convention("/")
        prop.string("openPath")?.let { set(it) }
    }

    val httpOpenUrl get() = when (openPath.get()) {
        "/" -> httpUrl.get()
        else -> "${httpUrl.get()}${openPath.get()}"
    }

    val jvmOpts = common.obj.strings {
        set(listOf("-server", "-Xmx2048m", "-XX:MaxPermSize=512M", "-Djava.awt.headless=true"))
        prop.strings("jvmOpts")?.let { set(it) }
    }

    val jvmAgents = JavaAgentResolver(aem).apply {
        prop.strings("jvmAgents")?.let { files(it) }
    }

    fun jvmAgents(options: JavaAgentResolver.() -> Unit) = jvmAgents.using(options)

    val jvmAgentOpt: String? get() = jvmAgents.files.joinToString(" ") { "-javaagent:$it" }.ifBlank { null }

    @Suppress("MagicNumber")
    val jvmDebugOpt: String? get() = when (debugPort.orNull) {
        in 1..65535 -> {
            val address = when {
                localManager.javaLauncher.get().metadata.languageVersion >= JavaLanguageVersion.of(9) -> {
                    when {
                        debugAddress.orNull == "*" -> "0.0.0.0:${debugPort.get()}"
                        debugAddress.orNull.isNullOrBlank() -> "${debugPort.get()}"
                        else -> "${debugAddress.orNull}:${debugPort.get()}"
                    }
                }
                else -> {
                    "${debugPort.get()}"
                }
            }
            "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$address"
        }
        else -> null
    }

    val jvmOptsString: String get() = (jvmOpts.get() + jvmDebugOpt + jvmAgentOpt).filterNot { it.isNullOrBlank() }.joinToString(" ")

    val javaExecutablePath: String get() = localManager.javaExecutablePath

    val startOpts = common.obj.strings {
        set(listOf())
        prop.strings("startOpts")?.let { set(it) }
    }

    val startOptsString: String get() = startOpts.get().filterNot { it.isNullOrBlank() }.joinToString(" ")

    val runModes = common.obj.strings {
        set(listOf())
        prop.strings("runModes")?.let { set(it) }
    }

    val runModesString: String get() = (runModes.get() + listOf(purpose.name.lowercase()))
        .filter { it.isNotBlank() }.joinToString(",")

    val dir: File get() = aem.localInstanceManager.instanceDir.get().asFile.resolve(purposeId)

    val controlDir: File get() = dir.resolve("control")

    val overridesDirs: List<File> get() = localManager.overrideDir.get().asFile.run { listOf(resolve("common"), resolve(purposeId)) }

    val jar: File? get() = quickstartDir.resolve("app").takeIf { it.exists() }?.listFiles(FileFilter { it.extension == "jar" })?.firstOrNull()

    val license: File get() = dir.resolve("license.properties")

    val quickstartDir: File get() = dir.resolve("crx-quickstart")

    val bundlesDir: File get() = quickstartDir.resolve("launchpad/felix")

    fun bundleDir(bundle: Bundle) = bundleDir(bundle.id.toInt())

    fun bundleDir(no: Int) = bundlesDir.resolve("bundle$no")

    val pidFile: File get() = quickstartDir.resolve("conf/cq.pid")

    val pid: Int get() = pidFile.takeIf { it.exists() }?.readText()
        ?.trim()?.ifBlank { null }?.toInt() ?: 0

    val logsDir: File get() = quickstartDir.resolve("logs")

    val stdoutLog: File get() = logsDir.resolve("stdout.log")

    val errorLog: File get() = logsDir.resolve("error.log")

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

    private fun readVersionFromJar() = readVersionFromExtractedJar() ?: readVersionFromProvidedJar() ?: AemVersion.UNKNOWN

    private fun readVersionFromExtractedJar() = jar?.name?.let { AemVersion.fromJarFileName(it) }

    private fun readVersionFromProvidedJar() = localManager.quickstart.distJar.let { AemVersion.fromJar(it) }

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

    val touched: Boolean get() = dir.exists()

    val created: Boolean get() = locked(LOCK_CREATE)

    val installDir get() = quickstartDir.resolve("install")

    val oakRun get() = OakRun(aem, this)

    private fun script(name: String, os: OperatingSystem = OperatingSystem.current()) = if (os.isWindows) {
        Script(this, listOf("cmd", "/C"), controlDir.resolve("$name.bat"), quickstartDir.resolve("bin/$name.bat"))
    } else {
        Script(this, listOf("sh"), controlDir.resolve("$name.sh"), quickstartDir.resolve("bin/$name"))
    }

    val localManager: LocalInstanceManager get() = aem.localInstanceManager

    fun create() = localManager.create(this)

    internal fun prepare() {
        cleanDir(true)
        unpackFiles()
        correctFiles()
        customizeWhenDown()
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
                    it.replace(
                        "java ",
                        when (file.extension) {
                            "bat" -> "%JAVA_EXECUTABLE% "
                            else -> "\$JAVA_EXECUTABLE "
                        }
                    )
                }
            }

        // Ensure that 'logs' directory exists
        logsDir.mkdirs()
    }

    private fun unpackFiles() {
        unpackQuickstartJar()

        logger.info("Copying quickstart license from '${localManager.license}' to '$license'")
        FileUtils.copyFile(localManager.license, license)
    }

    private fun unpackQuickstartJar() {
        logger.info("Unpacking quickstart from JAR '${localManager.jar}' to directory '$quickstartDir'")
        common.progressIndicator {
            step = "Copying quickstart JAR"
            val tmpJar = dir.resolve(localManager.jar.name)
            localManager.jar.copyTo(tmpJar, true)

            step = "Unpacking quickstart JAR: ${tmpJar.name} (${Formats.fileSize(tmpJar)})"
            aem.project.javaexec { spec ->
                spec.executable(localManager.javaExecutablePath)
                spec.workingDir = dir
                spec.mainClass.set("-jar")
                spec.args = listOf(tmpJar.absolutePath, "-unpack")
            }

            if (localManager.cleanJar.get()) {
                step = "Cleaning quickstart JAR"
                tmpJar.delete()
            }
        }
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

    internal fun customizeWhenDown() {
        aem.assetManager.copyDir(FILES_PATH, dir)
        copyOverrideFiles()
        expandFiles()
        copyInstallFiles()
        makeFilesExecutable()
    }

    internal fun customizeWhenUp() {
        auth.update()
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
        ) + localManager.expandProperties.get()

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

    val running: Boolean get() = created && checkStatus().running

    val runnable: Boolean get() = created && checkStatus().runnable

    val runningDir get() = aem.project.file(runningPath)

    val runningOther get() = available && (dir != runningDir)

    fun destroy() = localManager.destroy(this)

    val initialized: Boolean get() = locked(LOCK_INIT)

    internal fun init(callback: LocalInstance.() -> Unit) {
        apply(callback)
        lock(LOCK_INIT)
    }

    val auth by lazy { Auth(this) }

    private fun lockFile(name: String) = dir.resolve("$name.lock")

    internal fun lock(name: String) = FileOperations.lock(lockFile(name))

    internal fun locked(name: String): Boolean = lockFile(name).exists()

    fun resetPassword() {
        if (running) {
            throw LocalInstanceException("Instance is running so resetting password on $this is not possible!")
        }
        if (!initialized) {
            throw LocalInstanceException("Instance is not initialized so resetting password is not possible for $this!")
        }
        oakRun.resetPassword(user.get(), password.get())
    }

    override fun toString() = "LocalInstance(name='$name', httpUrl='${httpUrl.get()}')"

    init {
        user.apply {
            set(USER)
            finalizeValue() // only 'admin' is allowed
        }
    }

    companion object {

        const val FILES_PATH = "localInstance/defaults"

        const val SERVICE_PATH = "localInstance/service"

        const val ENVIRONMENT = CommonOptions.ENVIRONMENT_LOCAL

        const val USER = "admin"

        const val LOCK_CREATE = "create"

        const val LOCK_INIT = "init"
    }
}
