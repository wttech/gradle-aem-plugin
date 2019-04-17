package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.checks.ServiceChecker
import com.cognifide.gradle.aem.environment.docker.DockerTask
import com.cognifide.gradle.aem.environment.docker.Stack
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
        description = "Listen to httpd/dispatcher configuration changes and reloads Apache."
    }

    @Internal
    private val modificationsChannel = Channel<String>(Channel.UNLIMITED)

    @Internal
    private val requestToCheckStability = Channel<Any>(Channel.UNLIMITED)

    @Internal
    private val dirWatcher = DirMonitor(config.dispatcherConfPath, modificationsChannel)

    @Internal
    private val serviceAwait = ServiceChecker(aem)

    @TaskAction
    fun dev() {
        runBlocking {
            removeStackIfDeployed()
            deployStack()
            restartHttpd()
            requestToCheckStability.send(Date())
            dirWatcher.start()
            aem.logger.lifecycle("Listening for httpd configuration changes at: ${config.dispatcherConfPath}")
            reloadConfigurationOnChange()
            checkServiceStabilityOnReload()
        }
    }

    private fun deployStack() {
        stack.deploy(config.composeFilePath)
        val isContainerStarted = serviceAwait.awaitConditionObservingProgress("docker container - awaiting start", EnvDown.NETWORK_STOP_AWAIT_TIME) {
            stack.isContainerRunning(DISPATCHER_CONTAINER_NAME)
        }
        if (!isContainerStarted) {
            throw EnvironmentException("Failed to start docker container $DISPATCHER_CONTAINER_NAME" +
                    " after ${EnvDown.NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds.")
        }
    }

    private fun removeStackIfDeployed() {
        stack.rm()
        val isStopped = serviceAwait.awaitConditionObservingProgress("docker network - awaiting stop", EnvDown.NETWORK_STOP_AWAIT_TIME) { stack.isDown() }
        if (!isStopped) {
            throw EnvironmentException("Failed to stop docker stack after ${EnvDown.NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds." +
                    "\nPlease try to stop it manually by running: `docker stack rm ${Stack.STACK_NAME_DEFAULT}`")
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
            stack.exec(DISPATCHER_CONTAINER_NAME, HTTPD_RESTART_COMMAND, EXPECTED_HTTPD_RESTART_EXIT_CODE)
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
                val unavailableServices = serviceAwait.checkForUnavailableServices()
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
        private const val DISPATCHER_CONTAINER_NAME = "dispatcher"
        private const val HTTPD_RESTART_COMMAND = "/usr/local/apache2/bin/httpd -k restart"
        private const val EXPECTED_HTTPD_RESTART_EXIT_CODE = 0
    }
}