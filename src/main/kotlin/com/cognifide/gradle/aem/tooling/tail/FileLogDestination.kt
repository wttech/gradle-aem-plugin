package com.cognifide.gradle.aem.tooling.tail

import java.io.File
import java.io.FileWriter

class FileLogDestination(private val destinationFile: File) : LogDestination {

    init {
        clearFile()
    }

    override fun dump(logs: List<Log>) {
        if (logs.isEmpty()) return
        FileWriter(destinationFile.path, true).use { out ->
            logs.forEach { log ->
                out.append("${log.text}\n")
            }
        }
    }

    private fun clearFile() {
        destinationFile.bufferedWriter().use { out ->
            out.write("")
        }
    }
}