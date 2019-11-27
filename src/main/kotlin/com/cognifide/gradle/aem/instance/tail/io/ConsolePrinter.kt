package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.instance.tail.Log

class ConsolePrinter(
    private val instanceName: String,
    private val log: (String) -> Unit
) {
    init {
        log("Printing logs for $instanceName to console.")
    }

    fun dump(newLogs: List<Log>) = newLogs.forEach { log("[$instanceName]\t${it.logWithLocalTimestamp}") }
}
