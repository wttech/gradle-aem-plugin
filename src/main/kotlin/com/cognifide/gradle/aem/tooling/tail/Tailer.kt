package com.cognifide.gradle.aem.tooling.tail

import java.io.BufferedReader

class Tailer(private val source: LogSource, private val destination: LogDestination, val name: String = "") {

    private val parser = Parser()

    private var lastLogChecksum = ""

    fun tail() {
        val logs = source.processChunk { chunkReader ->
            parser.parseLogs(chunkReader)
        }
        val newLogs = determineNewLogs(logs)

        if (newLogs.isNotEmpty()) {
            lastLogChecksum = newLogs.last().checksum
            destination.dump(newLogs)
        }
    }

    private fun determineNewLogs(logs: List<Log>): List<Log> {
        val reversedLogs = logs.reversed()
        val lastFetchedLog = reversedLogs.find { log -> lastLogChecksum == log.checksum }
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
    fun <T> processChunk(parser: (BufferedReader) -> T): T {
        return nextReader().use(parser)
    }

    fun nextReader(): BufferedReader
}
