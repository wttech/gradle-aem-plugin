package com.cognifide.gradle.aem.common.build

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

class CollectingLogger {

    private val _entries = mutableListOf<LogEntry>()

    data class LogEntry(
        val level: LogLevel,
        val details: String,
        val summary: String? = null
    )

    fun log(level: LogLevel, details: String, summary: String? = null) {
        _entries += LogEntry(level, details, summary)
    }

    fun logTo(logger: Logger) {
        _entries.forEach { logger.log(it.level, it.details) }
    }

    val entries
        get() = _entries

    fun error(message: String) = log(LogLevel.ERROR, message)

    fun error(summary: String, details: String) = log(LogLevel.ERROR, details, summary)

    fun debug(message: String) = log(LogLevel.DEBUG, message)

    fun debug(summary: String, details: String) = log(LogLevel.DEBUG, details, summary)

    fun warn(message: String) = log(LogLevel.WARN, message)

    fun warn(summary: String, details: String) = log(LogLevel.WARN, details, summary)

    fun lifecycle(message: String) = log(LogLevel.LIFECYCLE, message)

    fun lifecycle(summary: String, details: String) = log(LogLevel.LIFECYCLE, details, summary)

    fun info(message: String) = log(LogLevel.INFO, message)

    fun info(summary: String, details: String) = log(LogLevel.INFO, details, summary)
}