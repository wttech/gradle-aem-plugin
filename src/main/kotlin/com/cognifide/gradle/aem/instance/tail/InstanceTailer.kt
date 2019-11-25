package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.instance.tail.io.FileDestination
import com.cognifide.gradle.aem.instance.tail.io.LogFiles
import com.cognifide.gradle.aem.instance.tail.io.UrlSource
import java.io.File
import java.nio.file.Paths
import kotlin.math.max
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class InstanceTailer(val aem: AemExtension) {

    /**
     * Directory where log files will be stored.
     */
    var rootDir: File = AemTask.temporaryDir(aem.project, "instanceTail")

    /**
     * Instances from which logs will be tailed.
     */
    var instances: List<Instance> = listOf()

    /**
     * Determines log file being tracked on AEM instance.
     */
    var logFilePath = aem.props.string("instance.tail.logFilePath") ?: "/logs/error.log"

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
                ?: aem.props.list("instance.tail.incidentLevels")
                ?: INCIDENT_LEVELS_DEFAULT
        val oldMillis = instance.property("instance.tail.incidentOld")?.toLong()
                ?: aem.props.long("instance.tail.incidentOld")
                ?: INCIDENT_OLD_DEFAULT

        isLevel(levels) && !isOlderThan(instance, oldMillis) && !logFilter.isExcluded(this)
    }

    /**
     * Path to file holding wildcard rules that will effectively deactivate notifications for desired exception.
     *
     * Changes in that file are automatically considered (tailer restart is not required).
     */
    var incidentFilter: File = (
            aem.props.string("instance.tail.incidentFilter")
                    ?.let { aem.project.file(it) }
                    ?: File(aem.configCommonDir, "instanceTail/incidentFilter.txt")
            ).apply { parentFile.mkdirs(); createNewFile() }

    /**
     * Time window in which exceptions will be aggregated and reported as single incident.
     */
    var incidentDelay = aem.props.long("instance.tail.incidentDelay") ?: 5000L

    /**
     * Determines how often logs will be polled from AEM instance.
     */
    var fetchInterval = aem.props.long("instance.tail.fetchInterval") ?: 500L

    var lockInterval = aem.props.long("instance.tail.lockInterval") ?: max(1000L + fetchInterval, 2000L)

    var linesChunkSize = aem.props.long("instance.tail.linesChunkSize") ?: 400L

    // https://sridharmandra.blogspot.com/2016/08/tail-aem-logs-in-browser.html
    fun errorLogEndpoint(instance: Instance): String {
        val fileName = logFilePath.replace("/", "%2F")
        val path = when {
            Formats.versionAtLeast(instance.version, "6.2.0") -> ENDPOINT_PATH
            else -> ENDPOINT_PATH_OLD
        }

        return "$path?tail=$linesChunkSize&name=$fileName"
    }

    val logFile: String
        get() = Paths.get(logFilePath).fileName.toString()

    fun incidentFilter(options: LogFilter.() -> Unit) {
        logFilter.apply(options)
    }

    private val logFiles = LogFiles(this)

    fun tail() {
        checkStartLock()

        runBlocking {
            startAll().forEach { tailer ->
                launch {
                    while (isActive) {
                        logFiles.lock()
                        tailer.tail()
                        delay(fetchInterval)
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

    private fun startAll(): List<LogTailer> {
        val notificationChannel = Channel<LogChunk>(Channel.UNLIMITED)
        val logNotifier = LogNotifier(notificationChannel, aem.notifier, logFiles)
        logNotifier.listenTailed()

        return instances.map { start(it, notificationChannel) }
    }

    private fun start(instance: Instance, notificationChannel: Channel<LogChunk>): LogTailer {
        val source = UrlSource(this, instance)
        val destination = FileDestination(instance.name, logFiles)
        val logAnalyzerChannel = Channel<Log>(Channel.UNLIMITED)

        val logAnalyzer = InstanceAnalyzer(this, instance, logAnalyzerChannel, notificationChannel)
        logAnalyzer.listenTailed()

        val logFile = logFiles.main(instance.name)
        aem.logger.lifecycle("Tailing logs to file: $logFile")

        return LogTailer(source, destination, logAnalyzerChannel)
    }

    companion object {

        const val ENDPOINT_PATH = "/system/console/slinglog/tailer.txt"

        const val ENDPOINT_PATH_OLD = "/bin/crxde/logs"

        val INCIDENT_LEVELS_DEFAULT = listOf("ERROR")

        const val INCIDENT_OLD_DEFAULT = 1000L * 10
    }
}
