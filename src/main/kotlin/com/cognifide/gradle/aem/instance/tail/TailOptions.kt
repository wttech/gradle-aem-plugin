package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Patterns
import java.nio.file.Paths
import javax.inject.Inject
import kotlin.math.max
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class TailOptions @Inject constructor(aem: AemExtension) {

    @Internal
    var fetchInterval = aem.props.long("aem.tail.fetchInterval") ?: FETCH_INTERVAL
    @Internal
    var lockInterval = aem.props.long("aem.tail.lockInterval") ?: max(1000L + fetchInterval, 2000L)
    @Internal
    var linesChunkSize = aem.props.long("aem.tail.linesChunkSize") ?: LOG_LINES_CHUNK_SIZE
    @Internal
    var notificationDelay = aem.props.long("aem.tail.notificationDelay") ?: NOTIFICATION_DELAY
    @Internal
    var logFilePath = aem.props.string("aem.tail.logFilePath") ?: LOG_FILE_PATH

    fun errorLogEndpoint() = "/system/console/slinglog/tailer.txt" +
        "?tail=$linesChunkSize" +
        "&name=${logFilePath.replace("/", "%2F")}"

    fun logFile() = Paths.get(logFilePath).fileName.toString()

    @Input
    val filters = mutableListOf<(Log) -> Boolean>()

    @Input
    val blacklistFiles = mutableListOf<String>()

    fun blacklistFile(filePath: String) {
        blacklistFiles += filePath
    }

    fun blacklist(filter: (Log) -> Boolean) {
        filters += filter
    }

    fun blacklist(filter: String) {
        filters += { Patterns.wildcard(it.source, filter) || Patterns.wildcard(it.message, filter) }
    }

    companion object {
        private const val FETCH_INTERVAL = 500L
        private const val LOG_LINES_CHUNK_SIZE = 400L
        private const val NOTIFICATION_DELAY = 5000L
        private const val LOG_FILE_PATH = "/logs/error.log"
        val BLACKLIST_FILES_DEFAULT = listOf(
            "aem/gradle/tail/errors-blacklist.log",
            "gradle/aem/tail/errors-blacklist.log"
        )
        const val USAGE_NOTIFICATION_INTERVAL: Long = 24 * 60 * 60 * 1000
    }
}