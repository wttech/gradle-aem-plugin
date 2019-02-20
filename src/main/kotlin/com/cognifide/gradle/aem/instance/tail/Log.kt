package com.cognifide.gradle.aem.instance.tail

import com.cognifide.gradle.aem.common.Formats
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Log(
    val text: String,
    val timestamp: LocalDateTime,
    val level: String,
    val source: String,
    messageLines: List<String>
) {

    val checksum = Formats.calculateChecksum(text)

    val message = messageLines.joinToString("\n")

    val messageChecksum = Formats.calculateChecksum(message)

    fun isError() = level == "ERROR"

    @Suppress("MagicNumber")
    fun isOlderThan(minutes: Long = 0, seconds: Long = 0, millis: Long = 0) =
            LocalDateTime.now()
                    .minusMinutes(minutes)
                    .minusSeconds(seconds)
                    .minusNanos(millis * 1000_000)
                    .isAfter(timestamp)

    companion object {

        private const val TIMESTAMP = """(?<timestamp>[0-9]{2}\.[0-9]{2}\.[0-9]{4}\s[0-9]{2}:[0-9]{2}:[0-9]{2}\.[0-9]{3})"""

        private const val LEVEL = """\*(?<level>[A-Z]+)\*"""

        private const val SOURCE = """(?<source>\[.*\])"""

        private const val MESSAGE = """(?<message>.*)"""

        private const val LOG_PATTERN = "$TIMESTAMP\\s$LEVEL\\s$SOURCE\\s$MESSAGE"

        private const val DATE_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss.SSS"

        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT)

        fun create(logLines: List<String>): Log {
            if (logLines.isEmpty() || logLines.first().isBlank()) throw TailException("Passed log entry is empty!")

            val fullLog = logLines.joinToString("\n")
            val result = matchLogLine(logLines.first())

            when (result) {
                null -> throw TailException("Passed text is not a log entry\nPattern:\n$LOG_PATTERN\nText:\n${logLines.first()}")
                else -> {
                    val (timestamp, level, source, message) = result.destructured
                    val followingMessageLines = logLines.slice(1 until logLines.size)
                    return Log(fullLog, parseTimestamp(timestamp), level, source, listOf(message) + followingMessageLines)
                }
            }
        }

        fun isFirstLineOfLog(text: String) = matchLogLine(text) != null

        fun parseTimestamp(timestamp: String): LocalDateTime {
            return LocalDateTime.parse(timestamp, DATE_TIME_FORMATTER)
                    ?: throw TailException("Invalid timestamp in log:\n$timestamp\n required format: $DATE_TIME_FORMAT")
        }

        private fun matchLogLine(text: String) = LOG_PATTERN.toRegex().matchEntire(text)
    }
}