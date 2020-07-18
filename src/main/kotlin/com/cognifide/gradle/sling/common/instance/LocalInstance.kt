package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.SlingVersion
import com.cognifide.gradle.sling.common.file.FileOperations
import com.cognifide.gradle.sling.common.file.ZipFile
import com.cognifide.gradle.sling.common.instance.local.Script
import com.cognifide.gradle.sling.common.instance.local.Status
import com.cognifide.gradle.common.utils.Formats
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.JavaVersion
import org.apache.commons.lang3.SystemUtils
import org.gradle.internal.os.OperatingSystem

class LocalInstance private constructor(sling: SlingExtension) : Instance(sling) {

    override var user: String = USER

    var debugPort: Int = 5005

    var debugAddress: String = ""

    var openPath: String = "/"

    val httpOpenUrl get() = when (openPath) {
        "/" -> httpUrl
        else -> "${httpUrl}$openPath"
    }

    private val debugSocketAddress: String
        get() = when (debugAddress) {
            "*" -> "0.0.0.0:$debugPort"
            "" -> "$debugPort"
            else -> "$debugAddress:$debugPort"
        }

    @get:JsonIgnore
    val jvmOptsDefaults: List<String>
        get() = mutableListOf<String>().apply {
            if (debugPort in 1..65535) {
                add(jvmDebugOpt)
            }
            if (password != Instance.PASSWORD_DEFAULT) {
                add("-Dadmin.password=$password")
            }
        }

    @get:JsonIgnore
    private val jvmDebugOpt: String
        get() = when {
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
    var startOpts: List<String> = listOf()

    @get:JsonProperty("startOpts")
    val startOptsString: String get() = startOpts.joinToString(" ")

    @get:JsonIgnore
    val runModesDefault get() = listOf(id.toLowerCase())

    @get:JsonIgnore
    var runModes: List<String> = listOf(ENVIRONMENT)

    @get:JsonProperty("runModes")
    val runModesString: String get() = (runModesDefault + runModes).joinToString(",")

    @get:JsonIgnore
    val dir get() = sling.localInstanceManager.rootDir.get().asFile.resolve(id)

    @get:JsonIgnore
    val overridesDirs get() = localManager.overrideDir.get().asFile.run { listOf(resolve("common"), resolve(id)) }

    @get:JsonIgnore
    val jar get() = dir.resolve("sling-starter.jar")

    @get:JsonIgnore
    val slingDir get() = dir.resolve("sling")

    @get:JsonIgnore
    val pid: Int get() = slingDir.resolve("conf/sling.pid")
            .takeIf { it.exists() }?.readText()?.trim()?.ifBlank { null }?.toInt() ?: 0

    private val startScript: Script get() = binScript("start")

    internal fun executeStartScript() {
        try {
            startScript.executeVerbosely()
        } catch (e: LocalInstanceException) {
            throw LocalInstanceException("Instance start script failed! Check resources like disk free space, open HTTP ports etc.", e)
        }
    }

    private val stopScript: Script get() = binScript("stop")

    internal fun executeStopScript() {
        val pidOrigin = pid
        try {
            stopScript.executeVerbosely()
        } catch (e: LocalInstanceException) {
            throw LocalInstanceException("Instance stop script failed! Consider killing process manually using PID: $pidOrigin.", e)
        }
    }

    private val statusScript: Script get() = binScript("status")

    @get:JsonIgnore
    val touched: Boolean get() = dir.exists()

    @get:JsonIgnore
    val created: Boolean get() = locked(LOCK_CREATE)

    @get:JsonIgnore
    val initialized: Boolean get() = locked(LOCK_INIT)

    @get:JsonIgnore
    val installDir get() = slingDir.resolve("install")

    private fun binScript(name: String, os: OperatingSystem = OperatingSystem.current()): Script {
        return if (os.isWindows) {
            Script(this, listOf("cmd", "/C"), dir.resolve("$name.bat"), slingDir.resolve("bin/$name.bat"))
        } else {
            Script(this, listOf("sh"), dir.resolve(name), slingDir.resolve("bin/$name"))
        }
    }

    @get:JsonIgnore
    val localManager: LocalInstanceManager get() = sling.localInstanceManager

    fun create() = localManager.create(this)

    internal fun prepare() {
        cleanDir(true)
        copyFiles()
        validateFiles()
        customize()
        lock(LOCK_CREATE)
    }

    private fun copyFiles() {
        dir.mkdirs()

        logger.info("Copying Sling Starter JAR '$jar' to directory '$slingDir'")
        localManager.starter.jar?.let { FileUtils.copyFile(it, jar) }
    }

    private fun validateFiles() {
        if (!jar.exists()) {
            throw LocalInstanceException("Instance JAR file not found at path: ${jar.absolutePath}. Is instance JAR URL configured?")
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

    internal fun customize() {
        sling.assetManager.copyDir(FILES_PATH, dir)

        overridesDirs.filter { it.exists() }.forEach {
            FileUtils.copyDirectory(it, dir)
        }

        val propertiesAll = mapOf("instance" to this) + properties + localManager.expandProperties.get()

        FileOperations.amendFiles(dir, localManager.expandFiles.get()) { file, source ->
            sling.prop.expand(source, propertiesAll, file.absolutePath)
        }

        val installFiles = localManager.install.files
        if (installFiles.isNotEmpty()) {
            installDir.mkdirs()
            installFiles.forEach { source ->
                val target = installDir.resolve(source.name)
                if (!target.exists()) {
                    logger.info("Copying Sling install file from '$source' to '$target'")
                    FileUtils.copyFileToDirectory(source, installDir)
                }
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
        if (!created) {
            return Status.UNKNOWN
        }

        return try {
            val procResult = statusScript.executeQuietly { withTimeoutMillis(localManager.statusTimeout.get()) }
            Status.byExitCode(procResult.exitValue).also { status ->
                logger.debug("Instance status of $this is: $status")
            }
        } catch (e: LocalInstanceException) {
            logger.info("Instance status not available: $this")
            logger.debug("Instance status error", e)
            Status.UNKNOWN
        }
    }

    @get:JsonIgnore
    val running: Boolean get() = created && checkStatus() == Status.RUNNING

    @get:JsonIgnore
    val runningDir get() = sling.project.file(runningPath)

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

    override fun toString() = "LocalInstance(name='$name', httpUrl='$httpUrl')"

    companion object {

        const val FILES_PATH = "localInstance/defaults"

        const val ENVIRONMENT = "local"

        const val USER = "admin"

        const val LOCK_CREATE = "create"

        const val LOCK_INIT = "init"

        fun create(sling: SlingExtension, httpUrl: String, configurer: LocalInstance.() -> Unit = {}): LocalInstance {
            return LocalInstance(sling).apply {
                val instanceUrl = InstanceUrl.parse(httpUrl)
                if (instanceUrl.user != USER) {
                    throw LocalInstanceException("User '${instanceUrl.user}' (other than 'admin') is not allowed while using local instance(s).")
                }

                this.httpUrl = instanceUrl.httpUrl
                this.password = instanceUrl.password
                this.id = instanceUrl.id
                this.debugPort = instanceUrl.debugPort
                this.env = sling.commonOptions.env.get()

                configurer()
                validate()
            }
        }
    }
}
