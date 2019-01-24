package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.tail.*
import com.cognifide.gradle.aem.instance.tail.io.FileDestination
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import com.cognifide.gradle.aem.instance.tail.io.UrlSource
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.shutdown.ShutdownHooks

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
@UseExperimental(ObsoleteCoroutinesApi::class)
open class Tail : AemDefaultTask() {

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies developer about unknown errors."
    }

    @Nested
    private val options = aem.project.objects.newInstance(TailOptions::class.java, aem)

    private val logFileCreator = LogFiles(options, aem, name)

    @TaskAction
    fun tail() {
        var shouldRunTailing = true
        ShutdownHooks.addShutdownHook {
            shouldRunTailing = false
        }
        checkStartLock()
        logger.lifecycle("Fetching logs every ${Formats.duration(options.fetchInterval)}")
        runBlocking {
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
    }

    fun config(configurer: TailOptions.() -> Unit) {
        options.apply(configurer)
    }

    fun showUsageNotification() {
        if (logFileCreator.shouldShowUsageNotification()) {
            aem.notifier.notify("Notice: log tailing is not running", "Consider starting it to easily monitor AEM logs:\nsh gradlew aemTail")
            aem.logger.lifecycle("*****************************************************************************************")
            aem.logger.lifecycle("*                                                                                       *")
            aem.logger.lifecycle("* Notice: Log tailing is not running!                                                   *")
            aem.logger.lifecycle("*         Consider starting it to easily monitor AEM logs: sh gradlew aemTail           *")
            aem.logger.lifecycle("*                                                                                       *")
            aem.logger.lifecycle("*****************************************************************************************")
        }
    }

    private fun checkStartLock() {
        if (logFileCreator.isLocked()) {
            logger.warn(
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
        logger.lifecycle("Creating log tailer for ${instance.name} (${instance.httpUrl}) -> ${logFileCreator.mainUri(instance.name)}")
        return Tailer(source, destination, logsAnalyzerChannel)
    }

    companion object {
        const val NAME = "aemTail"
    }
}
