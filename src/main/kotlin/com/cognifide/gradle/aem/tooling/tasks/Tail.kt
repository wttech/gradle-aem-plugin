package com.cognifide.gradle.aem.tooling.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.tooling.tail.Log
import com.cognifide.gradle.aem.tooling.tail.LogDestination
import com.cognifide.gradle.aem.tooling.tail.LogSource
import com.cognifide.gradle.aem.tooling.tail.Tailer
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlinx.coroutines.*
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.shutdown.ShutdownHooks

open class Tail : AemDefaultTask() {

    class AemLogSource(private val aemInstance: Instance) : LogSource {

        override fun nextReader(): BufferedReader {
            val connection = URL("${aemInstance.httpUrl}$ERROR_LOG_ENDPOINT").openConnection() as HttpURLConnection
            val auth = Base64.getEncoder().encode(("${aemInstance.user}:${aemInstance.password}").toByteArray()).toString(Charsets.UTF_8)
            connection.addRequestProperty("Authorization", "Basic $auth")
            connection.connect()
            return BufferedReader(InputStreamReader(connection.inputStream))
        }

        companion object {
            private const val NUMBER_OF_LINES_READ_EACH_TIME = 400
            const val ERROR_LOG_ENDPOINT = "/system/console/slinglog/tailer.txt?_dc=1520834477194&tail=$NUMBER_OF_LINES_READ_EACH_TIME&name=%2Flogs%2Ferror.log"
        }
    }

    class FileLogDestination(private val destinationFile: File) : LogDestination {

        init {
            clearFile()
        }

        override fun dump(logs: List<Log>) {
            FileWriter(destinationFile.path, true).use { out ->
                logs.forEach { log ->
                    out.append("${log.text}\n")
                }
            }
        }

        private fun clearFile() {
            destinationFile.bufferedWriter().use { out ->
                out.write("")
            }
        }
    }

    @TaskAction
    fun tail() {
        var shouldRunTailing = true
        ShutdownHooks.addShutdownHook {
            shouldRunTailing = false
        }
        logger.lifecycle("Tailing started man!")
        val tailers = prepareTailersForAllInstances()
        logger.lifecycle("Starting fetching logs every ${FETCH_INTERVAL_IN_MILISEC}ms")
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
    }
}
