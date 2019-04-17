package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.checks.ServiceChecker
import com.cognifide.gradle.aem.environment.docker.Container
import com.cognifide.gradle.aem.environment.docker.Stack
import com.cognifide.gradle.aem.environment.io.ConfigExpander
import java.text.SimpleDateFormat
import java.util.*
import org.buildobjects.process.ExternalProcessFailureException
import org.gradle.api.tasks.Internal

open class EnvTask : AemDefaultTask() {

    @Internal
    protected val stack = Stack()

    @Internal
    protected val config = ConfigExpander(aem)

    @Internal
    protected val options = aem.environmentOptions

    @Internal
    protected val serviceChecker = ServiceChecker(aem)

    @Internal
    private val httpdContainer = Container("${stack.name}_httpd")

    protected fun deployStack() {
        stack.deploy(config.composeFilePath)
        val isRunning = serviceChecker.awaitObservingProgress("httpd container - awaiting start", HTTPD_CONTAINER_AWAIT_TIME) {
            httpdContainer.isRunning()
        }
        if (!isRunning) {
            throw EnvironmentException("Failed to start '${httpdContainer.name}' container.")
        }
        restartHttpd()
    }

    protected fun removeStackIfDeployed() {
        stack.rm()
        val isStopped = serviceChecker.awaitObservingProgress("compose stack - awaiting stop", NETWORK_STOP_AWAIT_TIME) { stack.isDown() }
        if (!isStopped) {
            throw EnvironmentException("Failed to stop compose stack after ${NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds." +
                    "\nPlease try to stop it manually by running: `docker stack rm ${stack.name}`")
        }
    }

    protected fun restartHttpd() {
        try {
            httpdContainer.exec(HTTPD_RESTART_COMMAND, HTTPD_RESTART_EXIT_CODE)
            log("httpd restarted with new configuration. Checking service stability.")
        } catch (e: ExternalProcessFailureException) {
            log("Failed to reload httpd, exit code: ${e.exitValue}! Error:\n" +
                    "-------------------------------------------------------------------------------------------\n" +
                    e.stderr +
                    "-------------------------------------------------------------------------------------------")
        }
    }

    protected fun log(message: String) {
        aem.logger.lifecycle("${timestamp()} $message")
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }

    companion object {
        private const val NETWORK_STOP_AWAIT_TIME = 30000L
        private const val HTTPD_RESTART_COMMAND = "/usr/local/apache2/bin/httpd -k restart"
        private const val HTTPD_RESTART_EXIT_CODE = 0
        private const val HTTPD_CONTAINER_AWAIT_TIME = 5000L
    }
}