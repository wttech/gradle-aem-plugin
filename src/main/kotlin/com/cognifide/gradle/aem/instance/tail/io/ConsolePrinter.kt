package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.instance.tail.InstanceLoggingInfo
import com.cognifide.gradle.aem.instance.tail.Log

class ConsolePrinter(
    instance: InstanceLoggingInfo,
    private val log: (String) -> Unit
) {
    init {
        log("Printing logs for ${instance.name} to console.")
    }

    fun dump(newLogs: List<Log>) = newLogs.forEach { log(it.logWithLocalTimestamp) }

    companion object {
        fun devNull() = ConsolePrinter(InstanceLoggingInfo.default()) {}
    }
}
