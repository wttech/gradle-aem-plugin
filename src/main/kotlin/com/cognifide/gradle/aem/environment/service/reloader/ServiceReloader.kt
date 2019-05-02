package com.cognifide.gradle.aem.environment.service.reloader

import com.cognifide.gradle.aem.environment.Environment
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

@UseExperimental(ObsoleteCoroutinesApi::class)
open class ServiceReloader(val environment: Environment) {

    private val aem = environment.aem

    private val dirMonitor: DirMonitor
        get() = DirMonitor(environment.httpdConfDir, modificationsChannel)

    private val modificationsChannel = Channel<String>(Channel.UNLIMITED)

    private val requestToCheckStability = Channel<Any>(Channel.UNLIMITED)

    fun start() {
        runBlocking {
            environment.restart()
            requestToCheckStability.send(Date())
            dirMonitor.start()
            aem.logger.lifecycle("Listening for HTTPD configuration changes: ${environment.httpdConfDir}")
            reloadConfigurationOnChange()
            checkServiceStabilityOnReload()
        }
    }

    private fun CoroutineScope.reloadConfigurationOnChange() {
        launch(Dispatchers.IO) {
            while (true) {
                val changes = modificationsChannel.receiveAvailable()
                environment.log("Reloading httpd because of: ${changes.joinToString(", ")}")
                environment.httpd.restart()
                requestToCheckStability.send(Date())
            }
        }
    }

    private fun CoroutineScope.checkServiceStabilityOnReload(): Job {
        return launch {
            while (true) {
                requestToCheckStability.receiveAvailable()
                val unavailableServices = environment.serviceChecker.findUnavailable()
                if (unavailableServices.isEmpty()) {
                    environment.log("All stable, configuration update looks good.")
                } else {
                    environment.log("Services verification failed! URLs are unavailable or returned different response than expected:" +
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
        const val NAME = "environmentDev"
    }
}