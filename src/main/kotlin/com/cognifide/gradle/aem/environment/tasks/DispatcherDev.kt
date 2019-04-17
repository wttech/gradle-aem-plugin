package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.checks.ServiceChecker
import com.cognifide.gradle.aem.environment.docker.Container
import com.cognifide.gradle.aem.environment.docker.DockerTask
import com.cognifide.gradle.aem.environment.io.DirMonitor
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.buildobjects.process.ExternalProcessFailureException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
@UseExperimental(ObsoleteCoroutinesApi::class)
open class DispatcherDev : DockerTask() {

    init {
        description = "Listen to httpd/dispatcher configuration changes and reloads httpd."
    }

    @Internal
    private val modificationsChannel = Channel<String>(Channel.UNLIMITED)

    @Internal
    private val requestToCheckStability = Channel<Any>(Channel.UNLIMITED)

    @Internal
    private val dirWatcher = DirMonitor(config.dispatcherConfPath, modificationsChannel)

    @Internal
    private val serviceChecker = ServiceChecker(aem)

    @Internal
    private val httpdContainer = Container("${stack.name}_httpd")

    @TaskAction
    fun dev() {
        runBlocking {
            removeStackIfDeployed()
            deployStack()
            restartHttpd()
            requestToCheckStability.send(Date())
            dirWatcher.start()
            aem.logger.lifecycle("Listening for httpd/dispatcher configuration changes: ${config.dispatcherConfPath}")
            reloadConfigurationOnChange()
            checkServiceStabilityOnReload()
        }
    }

    private fun removeStackIfDeployed() {
        stack.rm()
        val isStopped = serviceChecker.awaitConditionObservingProgress("compose stack - awaiting stop", EnvDown.NETWORK_STOP_AWAIT_TIME) { stack.isDown() }
        if (!isStopped) {
            throw EnvironmentException("Failed to stop compose stack after ${EnvDown.NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds." +
                    "\nPlease try to stop it manually by running: `docker stack rm ${stack.name}`")
        }
    }

    private fun deployStack() {
        stack.deploy(config.composeFilePath)
        val isContainerStarted = serviceChecker.awaitConditionObservingProgress("httpd container - awaiting start", HTTPD_CONTAINER_AWAIT_TIME) {
            httpdContainer.isRunning()
        }
        if (!isContainerStarted) {
            throw EnvironmentException("Failed to start '${httpdContainer.name}' container " +
                    " after ${EnvDown.NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds.")
        }
    }

    private fun CoroutineScope.reloadConfigurationOnChange() {
        launch(Dispatchers.IO) {
            while (true) {
                val changes = modificationsChannel.receiveAvailable()
                log("Reloading httpd because of: ${changes.joinToString(", ")}")
                restartHttpd()
            }
        }
    }

    private suspend fun restartHttpd() {
        try {
            httpdContainer.exec(HTTPD_RESTART_COMMAND, HTTPD_RESTART_EXIT_CODE)
            log("httpd restarted with new configuration. Checking service stability.")
            requestToCheckStability.send(Date())
        } catch (e: ExternalProcessFailureException) {
            log("Failed to reload httpd, exit code: ${e.exitValue}! Error:\n" +
                    "-------------------------------------------------------------------------------------------\n" +
                    e.stderr +
                    "-------------------------------------------------------------------------------------------")
        }
    }

    private fun CoroutineScope.checkServiceStabilityOnReload(): Job {
        return launch {
            while (true) {
                requestToCheckStability.receiveAvailable()
                val unavailableServices = serviceChecker.checkForUnavailableServices()
                if (unavailableServices.isEmpty()) {
                    log("All stable, configuration update looks good.")
                } else {
                    log("Services verification failed! URLs are unavailable or returned different response than expected:" +
                            "\n${unavailableServices.joinToString("\n")}" +
                            "\nFix configuration to make it working again.")
                }
            }
        }
    }

    private suspend fun <E> ReceiveChannel<E>.receiveAvailable(): List<E> {
        val allMessages = mutableListOf<E>()
        allMessages.add(receive())
        var next = poll()
        while (next != null) {
            allMessages.add(next)
            next = poll()
        }
        return allMessages
    }

    private fun log(message: String) {
        aem.logger.lifecycle("${timestamp()} $message")
    }

    private fun timestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
    }

    companion object {
        const val NAME = "aemDispatcherDev"
        private const val HTTPD_RESTART_COMMAND = "/usr/local/apache2/bin/httpd -k restart"
        private const val HTTPD_RESTART_EXIT_CODE = 0
        private const val HTTPD_CONTAINER_AWAIT_TIME = 5000L
    }
}