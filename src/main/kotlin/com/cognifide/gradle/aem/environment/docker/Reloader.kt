package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.file.FileWatcher
import com.cognifide.gradle.aem.environment.Environment
import java.util.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.io.File

@UseExperimental(ObsoleteCoroutinesApi::class)
open class Reloader(val environment: Environment) {

    private val aem = environment.aem

    var dirs = mutableListOf<File>()

    private val fileChanges = Channel<FileWatcher.Event>(Channel.UNLIMITED)

    private val healthCheckRequests = Channel<Any>(Channel.UNLIMITED)

    fun dir(vararg files: File) = files.forEach { dirs.add(it) }

    fun dir(vararg paths: String) = paths.forEach { dirs.add(aem.project.file(it)) }

    fun configDir(vararg paths: String) = paths.forEach { dir(File(environment.configDir, it)) }

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
            reloadOnFileChanges()
            checkHealthAfterReload()
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun CoroutineScope.reloadOnFileChanges() = launch(Dispatchers.IO) {
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

    private fun CoroutineScope.checkHealthAfterReload() = launch {
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
