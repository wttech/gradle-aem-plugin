package com.cognifide.gradle.aem.common.instance.tail

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.capitalizeChar
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
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

        fun create(aem: AemExtension, info: LogInfo, logLines: List<String>): Log {

            if (logLines.isEmpty() || logLines.first().isBlank()) {
                throw TailerException("Passed log entry is empty!")
            }

            val fullLog = logLines.joinToString("\n")
            val result = matchLogLine(logLines.first())?.groups ?: throw TailerException("Cannot get any values from passed log entry!")

            val timestamp = result["timestamp"]?.value?.trim() ?: throw TailerException("Cannot get 'timestamp' value from log")
            val level = result["level"]?.value ?: throw TailerException("Cannot get 'level' value from log")
            val source = result["source"]?.value ?: throw TailerException("Cannot get 'source' value from log")
            val message = result["message"]?.value ?: throw TailerException("Cannot get 'message' value from log")
            val followingMessageLines = logLines.slice(1 until logLines.size)

            return Log(info, fullLog, parseTimestamp(timestamp, aem.instanceManager.tailer.datePattern.get(), info),
                level, source, listOf(message) + followingMessageLines)
        }

        fun isFirstLineOfLog(text: String) = matchLogLine(text) != null

        fun parseTimestamp(timestamp: String, dateTimeFormatter: DateTimeFormatter, logInfo: LogInfo = NoLogInfo()): ZonedDateTime {
            try {
                return LocalDateTime.parse(timestamp, dateTimeFormatter).atZone(logInfo.zoneId)
            } catch (e: DateTimeParseException) {
                throw TailerException(
                    "\nProbably date pattern doesn't match. " +
                        "Consider configuring it in property \"instance.tail.datePattern\" " +
                        "to match the other one configured on AEM instance(s)\n"
                )
            }
        }

        private fun matchLogLine(text: String) = LOG_PATTERN.toRegex().matchEntire(text)
    }
}
