package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.tail.io.FileDestination
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import com.cognifide.gradle.aem.instance.tail.io.UrlSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
@UseExperimental(ObsoleteCoroutinesApi::class)
class Tailers(
    name: String,
    private val aem: AemExtension,
    private val options: TailOptions
) {

    private val logFileCreator = LogFiles(name, aem, options)
    private var shouldRunTailing = true

    fun tail() {
        checkStartLock()
        runBlocking {
            tail()
        }
    }

    fun backgroundTail() {
        if (logFileCreator.isLocked()) {
            // early & quiet return when tailer is already running
            return
        }
        logFileCreator.lock()
        GlobalScope.launch {
            // non-blocking - will finish with build
            tail()
        }
    }

    fun stop() {
        shouldRunTailing = false
    }

    private fun CoroutineScope.tail() {
        createAllTailers().forEach { tailer ->
            launch {
                while (shouldRunTailing) {
                    logFileCreator.lock()
                    tailer.tail()
                    delay(options.fetchInterval)
                }
            }
        }
    }

    private fun checkStartLock() {
        if (logFileCreator.isLocked()) {
            aem.logger.warn(
                "Another instance of log tailer is running for this project. " +
                    "Stop it before starting a new one."
            )
            throw TailException("Another instance of log tailer is running for this project.")
        }
        logFileCreator.lock()
    }

    private fun createAllTailers(): List<Tailer> {
        val notificationChannel = Channel<ProblematicLogs>(Channel.UNLIMITED)
        LogNotifier(notificationChannel, aem.notifier, logFileCreator)
        return aem.instances.map { create(it, notificationChannel) }
    }

    private fun create(instance: Instance, notificationChannel: Channel<ProblematicLogs>): Tailer {
        val source = UrlSource(options, instance, aem)
        val destination = FileDestination(instance.name, logFileCreator)
        val logsAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)
        LogAnalyzer(options, instance.name, logsAnalyzerChannel, notificationChannel, Blacklist(options.filters, options.blacklistFiles))
        aem.logger.lifecycle("Creating log tailer for ${instance.name} (${instance.httpUrl}) -> ${logFileCreator.mainUri(instance.name)}")
        return Tailer(source, destination, logsAnalyzerChannel)
    }
}