package com.cognifide.gradle.aem.instance.tail.io

import com.cognifide.gradle.aem.instance.tail.InstanceLogInfo
import com.cognifide.gradle.aem.instance.tail.Log

class ConsolePrinter(
    instance: InstanceLogInfo,
    private val log: (String) -> Unit,
    private val incidentMaxAgeInMillis: Long = 5000
) {
    init {
        log("Printing logs for ${instance.name} to console.")
    }

    fun dump(newLogs: List<Log>) = newLogs.filterNot { it.isOlderThan(incidentMaxAgeInMillis) }.forEach { log(it.logWithLocalTimestamp) }

    companion object {
        fun none() = ConsolePrinter(InstanceLogInfo.none(), {})
    }
}
