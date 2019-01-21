package com.cognifide.gradle.aem.tooling.tail.io

import com.cognifide.gradle.aem.tooling.tail.Log
import com.cognifide.gradle.aem.tooling.tail.LogDestination

class FileDestination(
    private val instanceName: String,
    private val logFiles: LogFiles
) : LogDestination {

    init {
        logFiles.clearMain(instanceName)
    }

    override fun dump(logs: List<Log>) {
        if (logs.isEmpty()) return
        logFiles.writeToMain(instanceName) { out ->
            logs.forEach { log ->
                out.append("${log.text}\n")
            }
        }
    }
}