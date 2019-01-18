package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.tail.*
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        logger.lifecycle("Fetching logs every ${FETCH_INTERVAL_IN_MILLIS}ms")
        runBlocking {
            createAllTailers().forEach { tailer ->
                launch {
                    while (shouldRunTailing) {
                        tailer.tail()
                        delay(FETCH_INTERVAL_IN_MILLIS)
                    }
                }
            }
        }
    }

    private fun createAllTailers(): List<Tailer> {
        val notificationChannel = Channel<ProblematicLogs>(Channel.UNLIMITED)
        LogNotifier(notificationChannel, aem.notifier)
        return aem.instances.map { create(it, notificationChannel) }
    }

    private fun create(instance: Instance, notificationChannel: Channel<ProblematicLogs>): Tailer {
        val source = InstanceLogSource(instance)
        val destinationFile = AemTask.temporaryFile(aem.project, "$name/${instance.name}", "error.log")
        val destination = FileLogDestination(destinationFile)
        val logsAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)
        LogAnalyzer(instance.name, logsAnalyzerChannel, notificationChannel)
        logger.lifecycle("Creating log tailer for ${instance.name} (${instance.httpUrl}) -> ${destinationFile.path}")
        return Tailer(source, destination, logsAnalyzerChannel)
    }

    companion object {
        const val NAME = "aemTail"
        const val FETCH_INTERVAL_IN_MILLIS = 500L
        const val NUMBER_OF_LOG_LINES_READ_EACH_TIME = 400
        const val DELAY_TO_SHOW_ERROR_NOTIFICATION_IN_SEC = 4L
    }
}
