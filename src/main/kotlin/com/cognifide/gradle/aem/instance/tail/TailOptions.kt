package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.AemExtension
import java.nio.file.Paths
import kotlin.math.max

class TailOptions(val aem: AemExtension) {

    var fetchInterval = aem.props.long("aem.tail.fetchInterval") ?: 500L

    var lockInterval = aem.props.long("aem.tail.lockInterval") ?: max(1000L + fetchInterval, 2000L)

    var usageInterval = aem.props.long("aem.tai.usageInterval") ?: 24 * 60 * 60 * 1000

    var linesChunkSize = aem.props.long("aem.tail.linesChunkSize") ?: 400L

    var notificationDelay = aem.props.long("aem.tail.notificationDelay") ?: 5000L

    var logFilePath = aem.props.string("aem.tail.logFilePath") ?: "/logs/error.log"

    var logFilterPath = aem.props.string("aem.tail.logFilterPath") ?: "${aem.project.rootProject.file("aem/gradle/tail/logFilter.txt")}"

    val errorLogEndpoint: String
        get() = "/system/console/slinglog/tailer.txt" +
                "?tail=$linesChunkSize" +
                "&name=${logFilePath.replace("/", "%2F")}"

    fun logFile() = Paths.get(logFilePath).fileName.toString()

    val logFilter = LogFilter()

    fun logFilter(options: LogFilter.() -> Unit) {
        logFilter.apply(options)
    }
}