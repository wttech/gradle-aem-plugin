package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.tail.*
import com.cognifide.gradle.aem.instance.tail.io.FileDestination
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import com.cognifide.gradle.aem.instance.tail.io.UrlSource
import com.cognifide.gradle.aem.pkg.tasks.Deploy
import java.io.File
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.shutdown.ShutdownHooks

/**
 * TODO: Since coroutines API is still in experimental mode we would need to adapt to it's final API when released.
 * Please see https://github.com/Kotlin/kotlinx.coroutines/issues/632#issuecomment-425408865
 */
@UseExperimental(ObsoleteCoroutinesApi::class)
open class Tail : AemDefaultTask() {

    @Internal
    val options = TailOptions(aem)

    private val logFiles = LogFiles(options, name)

    init {
        description = "Tails logs from all configured instances (local & remote) and notifies about unknown errors."
    }

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

    private fun proposeStarting() {
        if (logFiles.isNotifiable()) {
            aem.notifier.notify("Log tailer not running", "Consider starting it")
        }
    }

    private fun startAll(): List<Tailer> {
        val notificationChannel = Channel<ProblematicLogs>(Channel.UNLIMITED)

        LogNotifier(notificationChannel, aem.notifier, logFiles)

        return aem.instances.map { start(it, notificationChannel) }
    }

    private fun start(instance: Instance, notificationChannel: Channel<ProblematicLogs>): Tailer {
        val source = UrlSource(options, instance)
        val destination = FileDestination(instance.name, logFiles)
        val logAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)
        val logFile = logFiles.main(instance.name)

        LogAnalyzer(options, instance.name, logAnalyzerChannel, notificationChannel)

        logger.lifecycle("Tailing logs to file: $logFile")

        return Tailer(source, destination, logAnalyzerChannel)
    }

    fun options(options: TailOptions.() -> Unit) {
        this.options.apply(options)
    }

    override fun projectEvaluated() {
        super.projectEvaluated()

        File(options.logFilterPath).takeIf { it.exists() }?.apply {
            options.logFilter.excludeFile(this)
        }
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        super.taskGraphReady(graph)

        if (graph.allTasks.any { it is Deploy }) {
            proposeStarting()
        }
    }

    companion object {
        const val NAME = "aemTail"
    }
}
