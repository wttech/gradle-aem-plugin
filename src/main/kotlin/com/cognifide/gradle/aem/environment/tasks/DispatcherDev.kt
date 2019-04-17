package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.environment.io.DirMonitor
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
@UseExperimental(ObsoleteCoroutinesApi::class)
open class DispatcherDev : EnvTask() {

    init {
        description = "Listen to httpd/dispatcher configuration changes and reloads httpd."
    }

    @Internal
    private val modificationsChannel = Channel<String>(Channel.UNLIMITED)

    @Internal
    private val requestToCheckStability = Channel<Any>(Channel.UNLIMITED)

    @Internal
    private val dirWatcher = DirMonitor(config.dispatcherConfPath, modificationsChannel)

    @TaskAction
    fun dev() {
        runBlocking {
            removeStackIfDeployed()
            deployStack()
            requestToCheckStability.send(Date())
            dirWatcher.start()
            aem.logger.lifecycle("Listening for httpd/dispatcher configuration changes: ${config.dispatcherConfPath}")
            reloadConfigurationOnChange()
            checkServiceStabilityOnReload()
        }
    }

    private fun CoroutineScope.reloadConfigurationOnChange() {
        launch(Dispatchers.IO) {
            while (true) {
                val changes = modificationsChannel.receiveAvailable()
                log("Reloading httpd because of: ${changes.joinToString(", ")}")
                restartHttpd()
                requestToCheckStability.send(Date())
            }
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

    companion object {
        const val NAME = "aemDispatcherDev"
    }
}