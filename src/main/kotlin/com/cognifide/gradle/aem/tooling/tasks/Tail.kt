package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.tooling.tail.*
import kotlinx.coroutines.*
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.shutdown.ShutdownHooks

open class Tail : AemDefaultTask() {

    @TaskAction
    fun tail() {
        var shouldRunTailing = true
        ShutdownHooks.addShutdownHook {
            shouldRunTailing = false
        }
        val tailers = prepareTailersForAllInstances()
        logger.lifecycle("Fetching logs every ${FETCH_INTERVAL_IN_MILISEC}ms")
        runBlocking {
            tailers.forEach { tailer ->
                launch {
                    while (shouldRunTailing) {
                        tailer.tail()
                        delay(FETCH_INTERVAL_IN_MILISEC)
                    }
                    logger.lifecycle("Finished fetching logs for ${tailer.name}")
                }
            }
        }
    }

    private fun prepareTailersForAllInstances(): List<Tailer> {
        return aem.instances.map { instance ->
            val source = AemLogSource(instance)
            val destinationFile = AemTask.temporaryFile(project, "$name/${instance.name}", "error.log")
            val destination = FileLogDestination(destinationFile)
            logger.lifecycle("Creating log tailer for ${instance.name} (${instance.httpUrl}) -> ${destinationFile.path}")
            Tailer(source, destination, instance.name)
        }
    }

    companion object {
        const val NAME = "aemTail"
        const val FETCH_INTERVAL_IN_MILISEC = 500L
        const val NUMBER_OF_LINES_READ_EACH_TIME = 400
    }
}
