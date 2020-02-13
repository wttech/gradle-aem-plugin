package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.aem.instance.tail.io.ConsolePrinter
import com.cognifide.gradle.aem.instance.tail.io.FileDestination
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import com.cognifide.gradle.aem.instance.tail.io.UrlSource
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.file.Paths
import kotlin.math.max

class InstanceTailer(val aem: AemExtension) {

    private val common = aem.common

    /**
     * Instances from which logs will be tailed.
     */
    val instances = aem.obj.list<Instance>()

    /**
     * Directory where log files will be stored.
     */
    val logStorageDir = aem.obj.dir {
        convention(aem.obj.buildDir(InstanceTail.NAME))
        aem.prop.file("instance.tail.logStorageDir")?.let { set(it) }
    }

    /**
     * Determines log file being tracked on AEM instance.
     */
    var logFilePath = aem.obj.string {
        convention("/logs/error.log")
        aem.prop.string("instance.tail.logFilePath")?.let { set(it) }
    }

    /**
     * Hook for tracking all log entries on each AEM instance.
     *
     * Useful for integrating external services like chats etc.
     */
    var logListener: Log.(Instance) -> Unit = {}

    /**
     * Log filter responsible for filtering incidents.
     */
    val logFilter = LogFilter()

    fun logFilter(options: LogFilter.() -> Unit) {
        logFilter.apply(options)
    }

    /**
     * Determines which log entries are considered as incidents.
     */
    var incidentChecker: Log.(Instance) -> Boolean = { instance ->
        val levels = Formats.toList(instance.property("instance.tail.incidentLevels"))
                ?: aem.prop.list("instance.tail.incidentLevels")
                ?: INCIDENT_LEVELS_DEFAULT
        val oldMillis = instance.property("instance.tail.incidentOld")?.toLong()
                ?: aem.prop.long("instance.tail.incidentOld")
                ?: INCIDENT_OLD_DEFAULT

        isLevel(levels) && !isOlderThan(oldMillis) && !logFilter.isExcluded(this)
    }

    /**
     * Path to file holding wildcard rules that will effectively deactivate notifications for desired exception.
     *
     * Changes in that file are automatically considered (tailer restart is not required).
     */
    val incidentFilter = aem.obj.file {
        convention(aem.instanceOptions.configDir.file("tail/incidentFilter.txt"))
        aem.prop.file("instance.tail.incidentFilter")?.let { set(it) }
    }

    /**
     * Indicates if tailer will print all logs to console.
     */
    val console = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.tail.console")?.let { set(it) }
    }

    /**
     * Time window in which exceptions will be aggregated and reported as single incident.
     */
    val incidentDelay = aem.obj.long {
        convention(5000L)
        aem.prop.long("instance.tail.incidentDelay")?.let { set(it) }
    }

    /**
     * Determines how often logs will be polled from AEM instance.
     */
    val fetchInterval = aem.obj.long {
        convention(500L)
        aem.prop.long("instance.tail.fetchInterval")?.let { set(it) }
    }

    val lockInterval = aem.obj.long {
        convention(fetchInterval.map { max(1000L + it, 2000L) })
        aem.prop.long("instance.tail.lockInterval")?.let { set(it) }
    }

    val linesChunkSize = aem.obj.long {
        convention(400L)
        aem.prop.long("instance.tail.linesChunkSize")?.let { set(it) }
    }

    // https://sridharmandra.blogspot.com/2016/08/tail-aem-logs-in-browser.html
    fun errorLogEndpoint(instance: Instance): String {
        val fileName = logFilePath.get().replace("/", "%2F")
        val path = when {
            Formats.versionAtLeast(instance.version, "6.2.0") -> ENDPOINT_PATH
            else -> ENDPOINT_PATH_OLD
        }

        return "$path?tail=$linesChunkSize&name=$fileName"
    }

    val logFile: String
        get() = Paths.get(logFilePath.get()).fileName.toString()

    fun incidentFilter(options: LogFilter.() -> Unit) {
        logFilter.apply(options)
    }

    private val logFiles = LogFiles(this)

    fun tail() {
        checkStartLock()
        initIncidentFilter()

        runBlocking {
            startAll().forEach { tailer ->
                launch {
                    while (isActive) {
                        logFiles.lock()
                        tailer.tail()
                        delay(fetchInterval.get())
                    }
                }
            }
        }
    }

    private fun checkStartLock() {
        if (logFiles.isLocked()) {
            throw InstanceTailerException("Another instance of log tailer is running for this project.")
        }
        logFiles.lock()
    }

    private fun initIncidentFilter() = incidentFilter.get().asFile.run {
        parentFile.mkdirs()
        createNewFile()
    }

    private fun startAll(): List<LogTailer> {
        val notificationChannel = Channel<LogChunk>(Channel.UNLIMITED)
        val logNotifier = LogNotifier(notificationChannel, common.notifier, logFiles)
        logNotifier.listenTailed()

        return instances.get().map { start(it, notificationChannel) }
    }

    private fun start(instance: Instance, notificationChannel: Channel<LogChunk>): LogTailer {
        val source = UrlSource(this, instance)
        val destination = FileDestination(instance.name, logFiles)
        val logAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)

        val logAnalyzer = InstanceAnalyzer(this, instance, logAnalyzerChannel, notificationChannel)
        logAnalyzer.listenTailed()

        val logFile = logFiles.main(instance.name)
        aem.logger.lifecycle("Tailing logs to file: $logFile")

        return LogTailer(source, destination, InstanceLogInfo.of(instance), logAnalyzerChannel, consolePrinter(instance))
    }

    private fun consolePrinter(instance: Instance) = if (console.get()) {
        ConsolePrinter(InstanceLogInfo.of(instance), { aem.logger.lifecycle(it) })
    } else {
        ConsolePrinter.none()
    }

    companion object {

        const val ENDPOINT_PATH = "/system/console/slinglog/tailer.txt"

        const val ENDPOINT_PATH_OLD = "/bin/crxde/logs"

        val INCIDENT_LEVELS_DEFAULT = listOf("ERROR")

        const val INCIDENT_OLD_DEFAULT = 1000L * 10
    }
}
