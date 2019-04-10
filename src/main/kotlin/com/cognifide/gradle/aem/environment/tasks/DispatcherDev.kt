package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.environment.io.DirWatcher
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.checks.ServiceAwait
import com.cognifide.gradle.aem.environment.docker.DockerTask
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.buildobjects.process.ExternalProcessFailureException
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class DispatcherDev : DockerTask() {

    init {
        description = "Listens to httpd/dispatcher configuration changes and reloads Apache."
    }

    @Internal
    private val modificationsChannel = Channel<List<String>>(Channel.UNLIMITED)

    @Internal
    private val requestToCheckStability = Channel<Any>(Channel.UNLIMITED)

    @Internal
    private val dirWatcher = DirWatcher(config.dispatcherConfPath, modificationsChannel)

    @Internal
    private val serviceAwait = ServiceAwait(aem)

    @TaskAction
    fun dev() {
        runBlocking {
            stack.rm()
            val isStopped = serviceAwait.awaitConditionObservingProgress("docker network - awaiting stop", EnvDown.NETWORK_STOP_AWAIT_TIME) { stack.isDown() }
            if (!isStopped) {
                throw EnvironmentException("Failed to stop docker stack after ${EnvDown.NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds." +
                        "\nPlease try to stop it manually by running: `docker stack rm ${options.docker.stackName}`")
            }
            stack.deploy(config.composeFilePath)
            requestToCheckStability.send(Date())
            dirWatcher.watch()
            aem.logger.lifecycle("Listening for httpd configuration changes at: ${config.dispatcherConfPath}")
            reloadConfigurationOnChange()
            checkServiceStability()
        }
    }

    private fun CoroutineScope.reloadConfigurationOnChange() {
        launch(Dispatchers.IO) {
            while (true) {
                val changes = modificationsChannel.receiveAvailable().flatten()
                log("Reloading httpd because of: ${changes.joinToString(", ")}")
                try {
                    stack.exec("dispatcher", HTTPD_RESTART_COMMAND, EXPECTED_HTTPD_RESTART_EXIT_CODE)
                    log("httpd restarted with new configuration. Checking service stability.")
                    requestToCheckStability.send(Date())
                } catch (e: ExternalProcessFailureException) {
                    log("Failed to reload httpd, exit code: ${e.exitValue}! Error:\n" +
                            "-------------------------------------------------------------------------------------------\n" +
                            e.stderr +
                            "-------------------------------------------------------------------------------------------")
                }
            }
        }
    }

    private fun CoroutineScope.checkServiceStability(): Job {
        return launch {
            while (true) {
                requestToCheckStability.receiveAvailable()
                val unavailableServices = serviceAwait.checkForUnavailableServices()
                if (unavailableServices.isEmpty()) {
                    log("All stable, configuration update looks good.")
                } else {
                    log("Services verification failed! Following URLs are still unavailable " +
                            "or returned different response than expected:\n${unavailableServices.joinToString("\n")}")
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
        private const val HTTPD_RESTART_COMMAND = "/usr/sbin/httpd -k restart"
        private const val EXPECTED_HTTPD_RESTART_EXIT_CODE = 129
    }
}