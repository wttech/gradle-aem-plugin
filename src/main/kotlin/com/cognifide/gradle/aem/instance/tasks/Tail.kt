package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
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
    private val options = TailOptions(aem)

    private val logFileCreator = LogFiles(options, aem, name)

    @TaskAction
    fun tail() {
        var shouldRunTailing = true
        ShutdownHooks.addShutdownHook {
            shouldRunTailing = false
        }
        checkStartLock()

        runBlocking {
            startAll().forEach { tailer ->
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
            aem.notifier.notify("Notice: log tailer not running", "Consider starting it")
        }
    }

    private fun checkStartLock() {
        if (logFileCreator.isLocked()) {
            throw TailException("Another instance of log tailer is running for this project.")
        }
        logFileCreator.lock()
    }

    private fun startAll(): List<Tailer> {
        val notificationChannel = Channel<ProblematicLogs>(Channel.UNLIMITED)

        LogNotifier(notificationChannel, aem.notifier, logFileCreator)

        return aem.instances.map { start(it, notificationChannel) }
    }

    private fun start(instance: Instance, notificationChannel: Channel<ProblematicLogs>): Tailer {
        val source = UrlSource(options, instance, aem)
        val destination = FileDestination(instance.name, logFileCreator)
        val logAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)
        val logFile = logFileCreator.main(instance.name)
        val blacklist = Blacklist(options.filters, options.blacklistFiles)

        LogAnalyzer(options, instance.name, logAnalyzerChannel, notificationChannel, blacklist)

        logger.lifecycle("Tailing logs to file: $logFile")

        return Tailer(source, destination, logAnalyzerChannel)
    }

    companion object {
        const val NAME = "aemTail"
    }
}
