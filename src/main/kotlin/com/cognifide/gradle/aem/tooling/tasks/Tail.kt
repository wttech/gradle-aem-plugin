package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.tail.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.shutdown.ShutdownHooks

@UseExperimental(ObsoleteCoroutinesApi::class)
open class Tail : AemDefaultTask() {

    @TaskAction
    fun tail() {
        var shouldRunTailing = true
        ShutdownHooks.addShutdownHook {
            shouldRunTailing = false
        }
        logger.lifecycle("Fetching logs every ${FETCH_INTERVAL_IN_MILISEC}ms")
        runBlocking {
            createAllTailers().forEach { tailer ->
                launch {
                    while (shouldRunTailing) {
                        tailer.tail()
                        delay(FETCH_INTERVAL_IN_MILISEC)
                    }
                }
            }
        }
    }

    private fun createAllTailers(): List<Tailer> {
        val notificationChannel = Channel<ProblematicLog>(Channel.UNLIMITED)
        GlobalScope.launch {
            notificationChannel.consumeEach {
                aem.notifier.notify("Errors from ${it.instanceName}", it.log.message)
            }
        }
        return aem.instances.map { create(it, notificationChannel) }
    }

    private fun create(instance: Instance, notificationChannel: Channel<ProblematicLog>): Tailer {
        val source = AemLogSource(instance)
        val destinationFile = AemTask.temporaryFile(aem.project, "$name/${instance.name}", "error.log")
        val destination = FileLogDestination(destinationFile)
        val logsAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)
        AemLogAnalyzer(instance.name, logsAnalyzerChannel, notificationChannel)
        logger.lifecycle("Creating log tailer for ${instance.name} (${instance.httpUrl}) -> ${destinationFile.path}")
        return Tailer(source, destination, logsAnalyzerChannel)
    }

    companion object {
        const val NAME = "aemTail"
        const val FETCH_INTERVAL_IN_MILISEC = 500L
        const val NUMBER_OF_LINES_READ_EACH_TIME = 400
    }
}
