package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.common.utils.Formats
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class Log(
    val instance: InstanceLogInfo = InstanceLogInfo.none(),
    val text: String,
    val timestamp: ZonedDateTime,
    val level: String,
    val source: String,
    messageLines: List<String>
) {

    val checksum = Formats.toMd5(text)

    val message = messageLines.joinToString("\n")

    val cause: String
        get() = message.splitToSequence("\n").firstOrNull()?.run { trim() }
                ?.substringAfter(" ")?.capitalize() ?: ""

    val logWithLocalTimestamp: String
        get() =
            "[${instance.name.padEnd(13)}]" +
                    "$LOGS_SEPARATOR${timestamp.toLocalDateTime().format(PRINT_DATE_TIME_FORMATTER)}" +
                    "$LOGS_SEPARATOR${level.padEnd(5)}" +
                    "$LOGS_SEPARATOR$source" +
                    "$LOGS_SEPARATOR$message"

    fun isLevel(vararg levels: String) = isLevel(levels.asIterable())

    fun isLevel(levels: Iterable<String>): Boolean = levels.any { it.equals(level, true) }

    fun isOlderThan(millis: Long): Boolean {
        val nowTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val diffMillis = ChronoUnit.MILLIS.between(timestamp, nowTimestamp)

        return diffMillis > millis
    }

    companion object {

        private const val TIMESTAMP = """(?<timestamp>[0-9]{2}\.[0-9]{2}\.[0-9]{4}\s[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3})"""

        private const val LEVEL = """\*(?<level>[A-Z]+)\*"""

        private const val SOURCE = """(?<source>\[.*\])"""

        private const val MESSAGE = """(?<message>.*)"""

        private const val LOG_PATTERN = "$TIMESTAMP\\s$LEVEL\\s$SOURCE\\s$MESSAGE"

        private const val LOGS_SEPARATOR = "  "

        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss.SSS")

        private val PRINT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        fun create(instance: InstanceLogInfo, logLines: List<String>): Log {
            if (logLines.isEmpty() || logLines.first().isBlank()) {
                throw InstanceTailerException("Passed log entry is empty!")
            }

            val fullLog = logLines.joinToString("\n")

            when (val result = matchLogLine(logLines.first())) {
                null -> throw InstanceTailerException("Passed text is not a log entry\nPattern:\n$LOG_PATTERN\nText:\n${logLines.first()}")
                else -> {
                    val (timestamp, level, source, message) = result.destructured
                    val followingMessageLines = logLines.slice(1 until logLines.size)
                    return Log(
                            instance,
                            fullLog,
                            parseTimestamp(timestamp, instance),
                            level,
                            source,
                            listOf(message) + followingMessageLines
                    )
                }
            }
        }

        fun isFirstLineOfLog(text: String) = matchLogLine(text) != null

        fun parseTimestamp(timestamp: String, instance: InstanceLogInfo = InstanceLogInfo.none()): ZonedDateTime {
            return LocalDateTime.parse(timestamp, DATE_TIME_FORMATTER).atZone(instance.zoneId)
                    ?: throw InstanceTailerException("Invalid timestamp in log:\n$timestamp" +
                            "\n required format: $DATE_TIME_FORMATTER")
        }

        private fun matchLogLine(text: String) = LOG_PATTERN.toRegex().matchEntire(text)
    }
}
