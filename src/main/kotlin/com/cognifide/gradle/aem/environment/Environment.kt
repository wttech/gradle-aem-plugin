package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.docker.domain.AemStack
import com.cognifide.gradle.aem.environment.docker.domain.HttpdContainer
import com.cognifide.gradle.aem.environment.service.checker.ServiceChecker
import com.cognifide.gradle.aem.environment.service.reloader.ServiceReloader
import org.gradle.util.GFileUtils
import java.io.File

class Environment(val aem: AemExtension) {

    val options = aem.config.environmentOptions

    val stack = AemStack(this)

    val httpd = HttpdContainer(this)

    val serviceReloader = ServiceReloader(this)

    val serviceChecker = ServiceChecker(this)

    val created: Boolean
        get() = options.createdLockFile.exists()

    val running: Boolean
        get() = created && stack.running && httpd.running

    fun up() {
        if (running) {
            aem.logger.info("Environment is already running!")
            return
        }

        aem.logger.info("Turning on: $this")

        customize()

        stack.deploy()
        httpd.deploy()

        lock()

        aem.logger.info("Turned on: $this")
    }

    fun down() {
        if (!running) {
            aem.logger.info("Environment is not yet running!")
            return
        }

        aem.logger.info("Turning off: $this")

        stack.undeploy()

        aem.logger.info("Turned off: $this")
    }

    fun restart() {
        down()
        up()
    }

    fun destroy() {
        aem.logger.info("Destroying: $this")

        options.rootDir.deleteRecursively()

        aem.logger.info("Destroyed: $this")
    }

    private fun customize() {
        aem.logger.info("Customizing AEM environment")

        with(options) {
            provideFiles()
            syncDockerComposeFile()
            ensureDirsExist()
        }
    }

    private fun lock() {
        FileOperations.lock(options.createdLockFile)
    }

    private fun EnvironmentOptions.provideFiles() {
        if (!dispatcherModuleFile.exists()) {
            GFileUtils.copyFile(dispatcherModuleSourceFile, dispatcherModuleFile)
        }
    }

    private fun EnvironmentOptions.syncDockerComposeFile() {
        aem.logger.info("Synchronizing Docker compose file: $dockerComposeSourceFile -> $dockerComposeFile")

        if (!dockerComposeSourceFile.exists()) {
            throw EnvironmentException("Docker compose file does not exist: $dockerComposeSourceFile")
        }

        GFileUtils.deleteFileQuietly(dockerComposeFile)
        GFileUtils.copyFile(dockerComposeSourceFile, dockerComposeFile)
    }

    private fun EnvironmentOptions.ensureDirsExist() {
        directories.forEach { dirPath ->
            val dir = File(rootDir, dirPath)
            if (!dir.exists()) {
                aem.logger.info("Creating AEM environment directory: $dir")
                GFileUtils.mkdirs(dir)
            }
        }
    }

    fun check() {
        serviceChecker.findUnavailable().apply {
            if (isNotEmpty()) {
                throw EnvironmentException("Services verification failed! URLs are unavailable or returned different " +
                        "response than expected:\n${joinToString("\n")}")
            }
        }
    }

    fun log(message: String) {
        aem.logger.lifecycle("${Formats.timestamp()} $message")
    }

    override fun toString(): String {
        return "Environment(root=${options.rootDir},running=$running)"
    }
}