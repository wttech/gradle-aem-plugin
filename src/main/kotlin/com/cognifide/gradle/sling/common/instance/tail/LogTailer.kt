package com.cognifide.gradle.sling.common.instance.tail

import com.cognifide.gradle.sling.common.instance.tail.io.ConsolePrinter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

class LogTailer(
    private val source: LogSource,
    private val destination: LogDestination,
    info: LogInfo = NoLogInfo(),
    private val logsAnalyzerChannel: SendChannel<Log>? = null,
    private val printer: ConsolePrinter = ConsolePrinter.none()
) {

    private val parser = LogParser(info)

    private var lastLogChecksum = ""

    fun tail() {
        val logs = source.readChunk(parser::parse)
        val newLogs = determineNewLogs(logs)

        if (newLogs.isNotEmpty()) {
            lastLogChecksum = newLogs.last().checksum
            sendLogsToBeAnalyzed(newLogs)
            destination.dump(newLogs)
            printer.dump(newLogs)
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
