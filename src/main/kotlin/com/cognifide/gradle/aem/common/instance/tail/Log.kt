package com.cognifide.gradle.aem.common.instance.tail

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.capitalizeChar
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class Log(
    val info: LogInfo = NoLogInfo(),
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
            ?.substringAfter(" ")?.capitalizeChar() ?: ""

    val logWithLocalTimestamp: String
        get() =
            "[${info.name.padEnd(13)}]" +
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

        private const val TIMESTAMP = """(?<timestamp>(^[\d[^(\*)]]+))"""

        private const val LEVEL = """\*(?<level>[A-Z]+)\*"""

        private const val SOURCE = """(?<source>\[.*\])"""

        private const val MESSAGE = """(?<message>.*)"""

        private const val LOG_PATTERN = "$TIMESTAMP\\s$LEVEL\\s$SOURCE\\s$MESSAGE"

        private const val LOGS_SEPARATOR = "  "

        private val PRINT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

        fun create(info: LogInfo, aem: AemExtension, logLines: List<String>): Log {

            if (logLines.isEmpty() || logLines.first().isBlank()) {
                throw TailerException("Passed log entry is empty!")
            }

            val fullLog = logLines.joinToString("\n")

            when (val result = matchLogLine(logLines.first())?.groups) {
                null -> throw TailerException("Passed text is not a log entry\nPattern:\n$LOG_PATTERN\nText:\n${logLines.first()}")
                else -> {
                    val followingMessageLines = logLines.slice(1 until logLines.size)
                    return Log(
                        info,
                        fullLog,
                        parseTimestamp(result.get("timestamp") ?.value ?.trim() ?: "", aem.datePattern.get(), info),
                        result.get("level")?.value ?: "",
                        result.get("source")?.value ?: "",
                        listOf(result.get("message")?.value ?: "") + followingMessageLines
                    )
                }
            }
        }

        fun isFirstLineOfLog(text: String) = matchLogLine(text) != null

        fun parseTimestamp(timestamp: String, dateTimeFormatter: DateTimeFormatter, logInfo: LogInfo = NoLogInfo()): ZonedDateTime {
            return LocalDateTime.parse(timestamp, dateTimeFormatter).atZone(logInfo.zoneId)
                ?: throw TailerException(
                    "Invalid timestamp in log:\n$timestamp" +
                        "\n required format: $dateTimeFormatter"
                )
        }

        private fun matchLogLine(text: String) = LOG_PATTERN.toRegex().matchEntire(text)
    }
}
