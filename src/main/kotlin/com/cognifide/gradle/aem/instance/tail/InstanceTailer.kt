package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.tail.io.FileDestination
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import com.cognifide.gradle.aem.instance.tail.io.UrlSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class InstanceTailer(val options: TailOptions, val instances: List<Instance>) {

    private val aem = options.aem

    private val logFiles = LogFiles(options)

    fun tail() {
        checkStartLock()

        runBlocking {
            startAll().forEach { tailer ->
                launch {
                    while (isActive) {
                        logFiles.lock()
                        tailer.tail()
                        delay(options.fetchInterval)
                    }
                }
            }
        }
    }

    private fun checkStartLock() {
        if (logFiles.isLocked()) {
            throw TailException("Another instance of log tailer is running for this project.")
        }
        logFiles.lock()
    }

    private fun startAll(): List<LogTailer> {
        val notificationChannel = Channel<LogChunk>(Channel.UNLIMITED)
        val logNotifier = LogNotifier(notificationChannel, aem.notifier, logFiles)
        logNotifier.listenTailed()

        return instances.map { start(it, notificationChannel) }
    }

    private fun start(instance: Instance, notificationChannel: Channel<LogChunk>): LogTailer {
        val source = UrlSource(options, instance)
        val destination = FileDestination(instance.name, logFiles)
        val logAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)

        val logAnalyzer = InstanceAnalyzer(options, instance, logAnalyzerChannel, notificationChannel)
        logAnalyzer.listenTailed()

        val logFile = logFiles.main(instance.name)
        aem.logger.lifecycle("Tailing logs to file: $logFile")

        return LogTailer(source, destination, logAnalyzerChannel)
    }
}