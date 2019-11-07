package com.cognifide.gradle.aem.environment.reloader

import com.cognifide.gradle.aem.common.file.FileWatcher
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.docker.Container
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import java.util.*

@UseExperimental(ObsoleteCoroutinesApi::class)
open class Reloader(val environment: Environment) {

    private val aem = environment.aem

    private val logger = aem.logger

    private val allContainers get() = environment.docker.containers.defined

    private val watchedContainers get() = allContainers.filter { it.devOptions.watchDirs.isNotEmpty() }

    private val fileChanges = Channel<ContainerFileEvent>(Channel.UNLIMITED)

    private val healthCheckRequests = Channel<Any>(Channel.UNLIMITED)

    val configured get() = watchedContainers.isNotEmpty()

    fun start() {
        runBlocking {
            watchContainerFileChanges()
            reloadOnFileChanges()
            checkHealthAfterReload()
        }
    }

    private fun CoroutineScope.watchContainerFileChanges() {
        watchedContainers.map { container ->
            FileWatcher(aem).apply {
                dirs = container.devOptions.watchDirs
                onChange = { event ->
                    GlobalScope.launch {
                        fileChanges.send(ContainerFileEvent(container, event))
                    }
                }

                logger.lifecycle("Watching for container '${container.name}' file changes in directories:\n" +
                        dirs.joinToString("\n"))

                start()
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun CoroutineScope.reloadOnFileChanges() = launch(Dispatchers.IO) {
        while (true) {
            fileChanges.receiveAvailable().fold(mutableMapOf<Container, MutableList<ContainerFileEvent>>(), { all, re ->
                all.getOrPut(re.container) { mutableListOf() }.add(re); all
            }).forEach { (container, changes) ->
                container.apply {
                    logger.lifecycle("Reloading container '$name' due to file changes:\n" +
                            changes.map { it.event }.joinToString("\n"))

                    try {
                        reload()
                    } catch (e: Exception) {
                        logger.error("Cannot reload container '$name' properly!", e)
                    }
                }
            }

            healthCheckRequests.send(Date())
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
