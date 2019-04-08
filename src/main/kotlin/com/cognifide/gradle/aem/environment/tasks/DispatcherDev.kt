package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.environment.DirWatcher
import com.cognifide.gradle.aem.environment.ServiceAwait
import com.cognifide.gradle.aem.environment.docker.DockerTask
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
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
        aem.logger.lifecycle("Listening for HTTPD configuration changes at: ${config.dispatcherConfPath}")
        runBlocking {
            dirWatcher.watch()
            reloadConfigurationOnChange()
            checkServiceStability()
        }
    }

    private fun CoroutineScope.reloadConfigurationOnChange() {
        launch(Dispatchers.IO) {
            while (true) {
                val changes = modificationsChannel.receive()
                log("Reloading HTTPD because of: ${changes.joinToString(", ")}")
                stack.exec("dispatcher", HTTPD_RESTART_COMMAND, EXPECTED_HTTPD_RESTART_EXIT_CODE)
                log("HTTPD restarted with new configuration. Checking service stability.")
                requestToCheckStability.send(Date())
                delay(ServiceAwait.AWAIT_DELAY_DEFAULT)
            }
        }
    }

    private fun CoroutineScope.checkServiceStability(): Job {
        return launch {
            while (true) {
                requestToCheckStability.receive()
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