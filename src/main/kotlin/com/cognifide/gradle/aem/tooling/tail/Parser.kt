package com.cognifide.gradle.aem.tooling.tail

import java.io.BufferedReader

class Parser {

    fun parseLogs(reader: BufferedReader): List<Log> {
        val firstLogLine = skipIncompleteLog(reader)
        return parseLogs(reader, firstLogLine)
    }

    private fun parseLogs(reader: BufferedReader, firstLineOfLog: String?): List<Log> {
        val (log, firstLineOfNextLog) = readLog(reader, firstLineOfLog)
        if (log == null) {
            return emptyList()
        }
        if (firstLineOfNextLog == null) {
            return listOf(log)
        }
        return listOf(log) + parseLogs(reader, firstLineOfNextLog)
    }

    private fun readLog(reader: BufferedReader, firstLogLine: String?): Pair<Log?, String?> {
        val (completeLogLines, firstLineOfNextLog) = readSubsequentLogLines(reader, firstLogLine)
        return if (completeLogLines.isNotEmpty()) {
            Log.create(completeLogLines) to firstLineOfNextLog
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

    private fun skipIncompleteLog(reader: BufferedReader): String? {
        val line: String? = readLine(reader)
        return when {
            line == null -> null
            Log.isFirstLineOfLog(line) -> line
            else -> skipIncompleteLog(reader)
        }
    }

    private fun readLine(reader: BufferedReader): String? = reader.readLine()
}