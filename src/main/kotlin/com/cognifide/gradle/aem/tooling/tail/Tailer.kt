package com.cognifide.gradle.aem.tooling.tail

import java.io.BufferedReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class Tailer(
    private val source: LogSource,
    private val destination: LogDestination,
    private val logsAnalyzerChannel: SendChannel<Log>? = null
) {

    private val parser = Parser()

    private var lastLogChecksum = ""

    fun tail() {
        val logs = source.readChunk(parser::parseLogs)
        val newLogs = determineNewLogs(logs)

        if (newLogs.isNotEmpty()) {
            lastLogChecksum = newLogs.last().checksum
            sendLogsToBeAnalyzed(newLogs)
            destination.dump(newLogs)
        }
    }

    private fun sendLogsToBeAnalyzed(newLogs: List<Log>) {
        if (logsAnalyzerChannel == null) return
        GlobalScope.launch {
            newLogs.forEach { logsAnalyzerChannel.send(it) }
        }
    }

    private fun determineNewLogs(logs: List<Log>): List<Log> {
        val lastFetchedLog = logs.asReversed().find { lastLogChecksum == it.checksum }
        return when {
            lastFetchedLog == null -> logs
            logs.last() === lastFetchedLog -> emptyList()
            else -> logs.slice(logs.indexOf(lastFetchedLog) + 1 until logs.size)
        }
    }
}

interface LogDestination {
    fun dump(logs: List<Log>)
}

interface LogSource {
    fun <T> readChunk(parser: (BufferedReader) -> T): T
}
