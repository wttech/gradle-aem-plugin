package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.docker.DockerContainer
import com.cognifide.gradle.aem.environment.docker.DockerStack
import com.cognifide.gradle.aem.environment.service.checker.ServiceChecker
import com.cognifide.gradle.aem.environment.service.reloader.ServiceReloader
import java.io.File
import org.buildobjects.process.ExternalProcessFailureException
import org.gradle.util.GFileUtils

class Environment(val aem: AemExtension) {

    val options = aem.config.environmentOptions

    val stack = DockerStack("aem")

    val httpdContainer = DockerContainer("aem_httpd")

    val serviceReloader = ServiceReloader(this)

    val serviceChecker = ServiceChecker(this)

    val created: Boolean
        get() = options.createdLockFile.exists()

    val running: Boolean
        get() = created && stack.running

    fun up() {
        if (running) {
            aem.logger.info("Environment is already running!")
            return
        }

        aem.logger.info("Turning on: $this")

        customize()
        deployStack()
        restartHttpd()
        createLock()

        aem.logger.info("Turned on: $this")
    }

    fun down() {
        if (!running) {
            aem.logger.info("Environment is not yet running!")
            return
        }

        aem.logger.info("Turning off: $this")

        undeployStack()

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

    private fun deployStack() {
        stack.deploy(options.dockerComposeFile.path)

        if (!serviceChecker.awaitObservingProgress("HTTPD container - awaiting start", HTTPD_CONTAINER_AWAIT_TIME) { httpdContainer.running }) {
            throw EnvironmentException("Failed to start '${httpdContainer.name}' container.")
        }
    }

    private fun undeployStack() {
        stack.rm()

        if (!serviceChecker.awaitObservingProgress("compose stack - awaiting stop", NETWORK_STOP_AWAIT_TIME) { !stack.running }) {
            throw EnvironmentException("Failed to stop compose stack after ${NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds." +
                    "\nPlease try to stop it manually by running: `docker stack rm ${stack.name}`")
        }
    }

    fun restartHttpd() {
        try {
            httpdContainer.exec(HTTPD_RESTART_COMMAND, HTTPD_RESTART_EXIT_CODE)
            log("HTTPD restarted with new configuration. Checking service stability.")
        } catch (e: ExternalProcessFailureException) {
            log("Failed to reload HTTPD, exit code: ${e.exitValue}! Error:\n${Formats.logMessage(e.stderr)}")
        }
    }

    fun customize() {
        aem.logger.info("Customizing AEM environment")

        with(options) {
            provideFiles()
            syncDockerComposeFile()
            ensureDirsExist()
        }
    }

    private fun createLock() {
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

    companion object {
        const val NETWORK_STOP_AWAIT_TIME = 30000L

        const val HTTPD_RESTART_COMMAND = "/usr/local/apache2/bin/httpd -k restart"

        const val HTTPD_RESTART_EXIT_CODE = 0

        const val HTTPD_CONTAINER_AWAIT_TIME = 10000L
    }
}