package com.cognifide.gradle.aem.instance.tail

import java.io.BufferedReader

class LogParser(
    private val instance: InstanceLoggingInfo = InstanceLoggingInfo.default()
) {

    fun parse(reader: BufferedReader): List<Log> {
        val firstLogLine = skipIncomplete(reader)
        return parse(reader, firstLogLine)
    }

    private fun parse(reader: BufferedReader, firstLineOfLog: String?): List<Log> {
        val (log, firstLineOfNextLog) = read(reader, firstLineOfLog)
        if (log == null) {
            return emptyList()
        }
        if (firstLineOfNextLog == null) {
            return listOf(log)
        }
        return listOf(log) + parse(reader, firstLineOfNextLog)
    }

    private fun read(reader: BufferedReader, firstLogLine: String?): Pair<Log?, String?> {
        val (completeLogLines, firstLineOfNextLog) = readSubsequentLogLines(reader, firstLogLine)
        return if (completeLogLines.isNotEmpty()) {
            Log.create(instance, completeLogLines) to firstLineOfNextLog
        } else {
            null to null
        }
    }

    private fun readSubsequentLogLines(reader: BufferedReader, logLine: String?): Pair<List<String>, String?> {
        val followingLine = readLine(reader)
        return when {
            logLine == null -> emptyList<String>() to null
            followingLine == null -> listOf(logLine) to null
            Log.isFirstLineOfLog(followingLine) -> listOf(logLine) to followingLine
            else -> {
                val lines = mutableListOf(logLine)
                val (followingLines, firstLineOfNextLog) = readSubsequentLogLines(reader, followingLine)
                lines += followingLines
                listOf(logLine) + followingLines to firstLineOfNextLog
            }
        }
    }

    private fun skipIncomplete(reader: BufferedReader): String? {
        val line: String? = readLine(reader)
        return when {
            line == null -> null
            Log.isFirstLineOfLog(line) -> line
            else -> skipIncomplete(reader)
        }
    }

    private fun readLine(reader: BufferedReader): String? = reader.readLine()
}
