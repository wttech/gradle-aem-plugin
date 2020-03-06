package com.cognifide.gradle.aem.common.instance.tail.io

import com.cognifide.gradle.aem.common.instance.tail.Log
import com.cognifide.gradle.aem.common.instance.tail.LogInfo
import com.cognifide.gradle.aem.common.instance.tail.NoLogInfo

class ConsolePrinter(
    logInfo: LogInfo,
    private val log: (String) -> Unit,
    private val incidentMaxAgeInMillis: Long = 5000
) {
    init {
        log("Printing logs for ${logInfo.name} to console.")
    }

    fun dump(newLogs: List<Log>) = newLogs.filterNot { it.isOlderThan(incidentMaxAgeInMillis) }.forEach { log(it.logWithLocalTimestamp) }

    companion object {
        fun none() = ConsolePrinter(NoLogInfo(), {})
    }
}
