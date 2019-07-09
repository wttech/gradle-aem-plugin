package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceSync
import org.gradle.api.logging.LogLevel

abstract class DefaultCheck(protected val group: CheckGroup) : Check {

    val runner = group.runner

    protected val aem = runner.aem

    val instance = group.instance

    val progress: CheckProgress
        get() = runner.progress(instance)

    val statusLogger = group.statusLogger

    var sync: InstanceSync = instance.sync.apply {
        http.connectionTimeout = 1000
        http.connectionRetries = false
    }

    override val status: String
        get() = statusLogger.entries.firstOrNull()?.summary ?: "Check passed"

    override val success: Boolean
        get() = statusLogger.entries.none { it.level == LogLevel.ERROR }

    fun <T : Any> state(value: T) = value.also { group.state(it) }

    fun logValues(values: Collection<Any>): String {
        val other = values.size - LOG_VALUES_COUNT
        return when {
            other > 0 -> values.take(LOG_VALUES_COUNT).joinToString("\n") + "\n... and other ($other)"
            else -> values.take(LOG_VALUES_COUNT).joinToString("\n")
        }
    }

    companion object {
        const val LOG_VALUES_COUNT = 10
    }
}