package com.cognifide.gradle.aem.environment.docker.domain

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileWatcher
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel

@UseExperimental(ObsoleteCoroutinesApi::class)
open class HttpdReloader(val aem: AemExtension) {

    private val environment = aem.environment

    private val fileChanges = Channel<FileWatcher.Event>(Channel.UNLIMITED)

    private val healthCheckRequests = Channel<Any>(Channel.UNLIMITED)

    private val fileWatcher = FileWatcher(aem).apply {
        dir = aem.environment.httpdConfDir
        onChange = { event ->
            GlobalScope.launch {
                fileChanges.send(event)
            }
        }
    }

    fun fileWatcher(options: FileWatcher.() -> Unit) {
        fileWatcher.apply(options)
    }

    fun start() {
        runBlocking {
            aem.logger.lifecycle("Watching for HTTPD configuration file changes in directory: ${environment.httpdConfDir}")

            fileWatcher.start()
            reloadHttpdOnFileChanges()
            checkHealthOnHttpdRestart()
        }
    }

    private fun CoroutineScope.reloadHttpdOnFileChanges() = launch(Dispatchers.IO) {
        while (true) {
            val changes = fileChanges.receiveAvailable()

            aem.logger.lifecycle("Reloading HTTP service due to file changes:\n${changes.joinToString("\n")}")

            if (environment.httpd.restart(verbose = false)) {
                environment.clean()
                healthCheckRequests.send(Date())
            }
        }
    }

    private fun CoroutineScope.checkHealthOnHttpdRestart() = launch {
        while (true) {
            healthCheckRequests.receiveAvailable()
            environment.check(false)
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
}