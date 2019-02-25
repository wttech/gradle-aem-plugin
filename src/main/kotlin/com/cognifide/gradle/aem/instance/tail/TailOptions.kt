package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.instance.Instance
import java.nio.file.Paths
import kotlin.math.max

class TailOptions(val aem: AemExtension, val taskName: String) {

    /**
     * Determines log file being tracked on AEM instance.
     */
    var logFilePath = aem.props.string("aem.tail.logFilePath") ?: "/logs/error.log"

    /**
     * Hook for tracking all log entries on each AEM instance.
     *
     * Useful for integrating external services like chats etc.
     */
    var logListener: Log.(Instance) -> Unit = {}

    /**
     * Log filter responsible for filtering incidents.
     */
    val incidentFilter = LogFilter()

    /**
     * Determines which log entries are considered as incidents.
     */
    var incidentChecker: Log.(Instance) -> Boolean = { instance ->
        val levels = Formats.toList(instance.string("tail.incidentLevels"))
                ?: aem.props.list("aem.tail.incidentLevels")
                ?: listOf("ERROR")
        val oldMillis = instance.string("tail.incidentOlderThan")?.toLong()
                ?: aem.props.long("aem.tail.incidentOlderThan")
                ?: 1000L * 10

        isLevel(levels) && !isOlderThan(instance, oldMillis) && !incidentFilter.isExcluded(this)
    }

    /**
     * Path to file holding wildcard rules that will effectively deactivate notifications for desired exception.
     *
     * Changes in that file are automatically considered (tailer restart is not required).
     */
    var incidentFilterPath = aem.props.string("aem.tail.incidentFilterPath")
            ?: "${aem.project.rootProject.file("aem/gradle/tail/incidentFilter.txt")}"

    /**
     * Time window in which exceptions will be aggregated and reported as single incident.
     */
    var incidentDelay = aem.props.long("aem.tail.incidentDelay") ?: 5000L

    /**
     * Determines how often logs will be polled from AEM instance.
     */
    var fetchInterval = aem.props.long("aem.tail.fetchInterval") ?: 500L

    var lockInterval = aem.props.long("aem.tail.lockInterval") ?: max(1000L + fetchInterval, 2000L)

    var linesChunkSize = aem.props.long("aem.tail.linesChunkSize") ?: 400L

    val errorLogEndpoint: String
        get() = "/system/console/slinglog/tailer.txt" +
                "?tail=$linesChunkSize" +
                "&name=${logFilePath.replace("/", "%2F")}"

    fun logFile() = Paths.get(logFilePath).fileName.toString()

    fun incidentFilter(options: LogFilter.() -> Unit) {
        incidentFilter.apply(options)
    }
}