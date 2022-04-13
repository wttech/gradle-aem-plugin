package com.cognifide.gradle.aem.common.instance.tail

import com.cognifide.gradle.aem.AemExtension
import java.io.BufferedReader

class LogParser(val aem: AemExtension, private val info: LogInfo = NoLogInfo()) {
    fun parse(reader: BufferedReader): List<Log> {
        val firstLogLine = skipIncomplete(reader)
        return parse(reader, firstLogLine, listOf())
    }

    private tailrec fun parse(reader: BufferedReader, firstLineOfLog: String?, result: List<Log>): List<Log> {
        val (log, firstLineOfNextLog) = read(reader, firstLineOfLog)
        if (log == null) {
            return result
        }
        if (firstLineOfNextLog == null) {
            return result + log
        }
        return parse(reader, firstLineOfNextLog, result + log)
    }

    private fun read(reader: BufferedReader, firstLogLine: String?): Pair<Log?, String?> {
        val (completeLogLines, firstLineOfNextLog) = readSubsequentLogLines(reader, firstLogLine?.let { listOf(it) } ?: emptyList<String>())
        return if (completeLogLines.isNotEmpty()) {
            Log.create(info, aem, completeLogLines) to firstLineOfNextLog
        } else {
            null to null
        }
    }

    private tailrec fun readSubsequentLogLines(reader: BufferedReader, logLines: List<String>): Pair<List<String>, String?> {
        val followingLine = readLine(reader)
        return when {
            logLines.isEmpty() -> logLines to null
            followingLine == null -> logLines to null
            Log.isFirstLineOfLog(followingLine) -> logLines to followingLine
            else -> readSubsequentLogLines(reader, logLines + followingLine)
        }
    }

    private tailrec fun skipIncomplete(reader: BufferedReader): String? {
        val line: String? = readLine(reader)
        return when {
            line == null -> null
            Log.isFirstLineOfLog(line) -> line
            else -> skipIncomplete(reader)
        }
    }

    private fun readLine(reader: BufferedReader): String? = reader.readLine()
}
