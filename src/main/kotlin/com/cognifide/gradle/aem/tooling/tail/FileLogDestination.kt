package com.cognifide.gradle.aem.tooling.tail

class FileLogDestination(
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