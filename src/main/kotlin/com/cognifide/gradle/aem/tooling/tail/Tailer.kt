package com.cognifide.gradle.aem.tooling.tail

import java.io.BufferedReader

class Tailer(private val source: LogSource, private val destination: LogDestination, val name: String = "") {

    private val parser = Parser()

    private var lastLogChecksum = ""

    fun tail() {
        val logs = source.readChunk(parser::parseLogs)
        val newLogs = determineNewLogs(logs)

        if (newLogs.isNotEmpty()) {
            lastLogChecksum = newLogs.last().checksum

            destination.dump(newLogs)
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

//    class Analyzer(private val notifier: NotifierFacade) {
//        fun reviewForErrors(newLogs: List<Log>) {
//            newLogs.filter { it.level == "ERROR" }.forEach {
//                notifier.notify("Error on ", it.text)
//            }
//        }
//    }
}

interface LogDestination {
    fun dump(logs: List<Log>)
}

interface LogSource {
    fun <T> readChunk(parser: (BufferedReader) -> T): T
}
