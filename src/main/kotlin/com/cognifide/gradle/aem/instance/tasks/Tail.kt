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

    private val logFileCreator = LogFiles(aem, name)

    @Nested
    private val config = TailConfig()

    fun config(configurer: TailConfig.() -> Unit) {
        config.apply(configurer)
    }

    @TaskAction
    fun tail() {
        var shouldRunTailing = true
        ShutdownHooks.addShutdownHook {
            shouldRunTailing = false
        }

        logger.lifecycle("Fetching logs every ${Formats.duration(FETCH_INTERVAL)}")
        runBlocking {
            createAllTailers().forEach { tailer ->
                launch {
                    while (shouldRunTailing) {
                        tailer.tail()
                        delay(FETCH_INTERVAL)
                    }
                }
            }
        }
    }

    private fun createAllTailers(): List<Tailer> {
        val notificationChannel = Channel<ProblematicLogs>(Channel.UNLIMITED)
        LogNotifier(notificationChannel, aem.notifier, logFileCreator)
        return aem.instances.map { create(it, notificationChannel) }
    }

    private fun create(instance: Instance, notificationChannel: Channel<ProblematicLogs>): Tailer {
        val source = UrlSource(instance)
        val destination = FileDestination(instance.name, logFileCreator)
        val logsAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)
        LogAnalyzer(instance.name, logsAnalyzerChannel, notificationChannel, Blacklist(config.filters, config.blacklistFiles))
        logger.lifecycle("Creating log tailer for ${instance.name} (${instance.httpUrl}) -> ${logFileCreator.mainUri(instance.name)}")
        return Tailer(source, destination, logsAnalyzerChannel)
    }

    companion object {
        const val NAME = "aemTail"
        const val FETCH_INTERVAL = 500L
        const val LOG_LINES_CHUNK_SIZE = 400L
        const val NOTIFICATION_DELAY = 5000L
        const val LOG_FILE = "error.log"
        const val LOG_FILE_PATH = "%2Flogs%2F$LOG_FILE"
        const val ERROR_LOG_ENDPOINT = "/system/console/slinglog/tailer.txt" +
            "?_dc=1520834477194" +
            "&tail=$LOG_LINES_CHUNK_SIZE" +
            "&name=$LOG_FILE_PATH"
        val BLACKLIST_FILES_DEFAULT = listOf(
                "aem/gradle/tail/errors-blacklist.log",
                "gradle/aem/tail/errors-blacklist.log"
        )
    }
}
