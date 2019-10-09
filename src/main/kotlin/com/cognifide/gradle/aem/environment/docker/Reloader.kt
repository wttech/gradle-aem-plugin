package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileWatcher
import com.cognifide.gradle.aem.environment.Environment
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File

@UseExperimental(ObsoleteCoroutinesApi::class)
open class Reloader(val aem: AemExtension) {

    var dirs = mutableListOf(
            File(aem.configCommonDir, "${Environment.ENVIRONMENT_DIR}/httpd/conf")
    )

    private val environment = aem.environment

    private val fileChanges = Channel<FileWatcher.Event>(Channel.UNLIMITED)

    private val healthCheckRequests = Channel<Any>(Channel.UNLIMITED)

    private val fileWatcher = FileWatcher(aem).apply {
        dirs = this@Reloader.dirs
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
            aem.logger.lifecycle("Watching for file changes in directories:\n${dirs.joinToString("\n")}")

            fileWatcher.start()
            reloadHttpdOnFileChanges()
            checkHealthOnHttpdRestart()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun CoroutineScope.reloadHttpdOnFileChanges() = launch(Dispatchers.IO) {
        while (true) {
            val changes = fileChanges.receiveAvailable()

            aem.logger.lifecycle("Reloading environment due to file changes:\n${changes.joinToString("\n")}")

            try {
                environment.reload()
                healthCheckRequests.send(Date())
            } catch (e: Exception) {
                aem.logger.error("Cannot reload environment properly", e)
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
