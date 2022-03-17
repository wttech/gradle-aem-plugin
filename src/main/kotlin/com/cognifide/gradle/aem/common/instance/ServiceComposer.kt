package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.common.file.FileOperations
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.gradle.internal.os.OperatingSystem
import java.io.File

/**
 * System service related options.
 */
class ServiceComposer(val manager: LocalInstanceManager) {

    private val aem = manager.aem

    private val logger = aem.logger

    /**
     * Path in which local service config files and scripts will be generated.
     */
    val configDir = aem.obj.dir {
        convention(manager.rootDir.dir("service"))
        aem.prop.file("localInstance.service.configDir")?.let { set(it) }
    }

    val overrideDir = aem.obj.dir {
        convention(manager.configDir.dir("service"))
        aem.prop.file("localInstance.service.overrideDir")?.let { set(it) }
    }

    val executableFiles = aem.obj.strings {
        set(listOf("*.sh"))
    }

    /**
     * System user used to run instance.
     */
    val user = aem.obj.string {
        convention("aem")
        aem.prop.string("localInstance.service.user")?.let { set(it) }
    }

    /**
     * System group of user used to run instance.
     */
    val group = aem.obj.string {
        convention("aem")
        aem.prop.string("localInstance.service.group")?.let { set(it) }
    }

    /**
     * Controls number of file descriptors allowed.
     */
    val limitNoFile = aem.obj.int {
        convention(20000)
        aem.prop.int("localInstance.service.limitNoFile")?.let { set(it) }
    }

    /**
     * Environment variables loader command.
     */
    val environmentCommand = aem.obj.string {
        convention(". /etc/profile")
        aem.prop.string("localInstance.service.environmentCommand")?.let { set(it) }
    }

    val projectPath get() = StringUtils.appendIfMissing(aem.project.path, ":")

    /**
     * Build command relative to root project used to start process which runs AEM instance as a system service.
     */
    val startCommand = aem.obj.string {
        convention("sh gradlew -i --console=plain ${projectPath}instanceUp")
        aem.prop.string("localInstance.service.startCommand")?.let { set(it) }
    }

    /**
     * Build command relative to root project used to stop process which runs AEM instance as a system service.
     */
    val stopCommand = aem.obj.string {
        convention("sh gradlew -i --console=plain ${projectPath}instanceDown")
        aem.prop.string("localInstance.service.stopCommand")?.let { set(it) }
    }

    /**
     * Build command relative to root project used to describe status of  AEM instance running as a system service.
     */
    val statusCommand = aem.obj.string {
        convention("sh gradlew -q --console=plain ${projectPath}instanceStatus")
        aem.prop.string("localInstance.service.statusCommand")?.let { set(it) }
    }

    /**
     * Effective values (shorthand)
     */
    val opts get() = mapOf(
        "dir" to configDir.get().asFile,
        "user" to user.orNull,
        "group" to group.orNull,
        "limitNoFile" to limitNoFile.orNull,
        "environmentCommand" to environmentCommand.orNull,
        "startCommand" to startCommand.orNull,
        "stopCommand" to stopCommand.orNull,
        "statusCommand" to statusCommand.orNull
    )

    fun compose() {
        val dir = configDir.get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }
        aem.assetManager.copyDir(LocalInstance.SERVICE_PATH, dir)
        if (overrideDir.get().asFile.exists()) {
            FileUtils.copyDirectory(overrideDir.get().asFile, dir)
        }
        val props = mapOf("service" to this)
        aem.project.fileTree(dir).forEach { file ->
            FileOperations.amendFile(file) { content ->
                aem.prop.expand(content, props, file.absolutePath)
            }
        }
        makeExecutable(dir)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun makeExecutable(dir: File) {
        if (OperatingSystem.current().isWindows) {
            return
        }

        aem.project.fileTree(dir)
            .matching { it.include(executableFiles.get()) }
            .forEach { file ->
                try {
                    FileOperations.makeExecutable(file)
                } catch (e: Exception) {
                    logger.warn("Cannot make file '$file' executable!", e)
                }
            }
    }
}
