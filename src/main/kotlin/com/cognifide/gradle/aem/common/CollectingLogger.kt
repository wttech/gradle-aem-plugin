package com.cognifide.gradle.aem.common

import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.Logger

class CollectingLogger {

    private val _logEntries = mutableListOf<LogEntry>()

    data class LogEntry(val level: LogLevel, val message: String)

    fun log(level: LogLevel, message: String) {
        _logEntries += LogEntry(level, message)
    }

    fun logTo(logger: Logger) {
        _logEntries.forEach { logger.log(it.level, it.message) }
    }

    val logEntries
        get() = _logEntries

    fun error(message: String) = log(LogLevel.ERROR, message)

    fun debug(message: String) = log(LogLevel.DEBUG, message)

    fun warn(message: String) = log(LogLevel.WARN, message)

    fun lifecycle(message: String) = log(LogLevel.LIFECYCLE, message)

    fun info(message: String) = log(LogLevel.INFO, message)
}