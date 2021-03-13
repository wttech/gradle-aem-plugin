package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.common.utils.using
import org.gradle.api.logging.LogLevel

abstract class DefaultCheck(protected val group: CheckGroup) : Check {

    val runner = group.runner

    protected val aem = runner.aem

    protected val common = aem.common

    protected val logger = aem.logger

    val instance = group.instance

    val progress: CheckProgress get() = runner.progress(instance)

    val statusLogger = group.statusLogger

    var sync: InstanceSync = instance.sync.apply {
        http.connectionTimeout.convention(5_000)
        http.connectionRetries.convention(false)
    }

    fun sync(callback: InstanceSync.() -> Unit) = sync.using(callback)

    val name get() = this::class.simpleName!!.removeSuffix("Check").decapitalize()

    override val enabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.check.$name.enabled")?.let { set(it) }
    }

    val logValuesCount = aem.obj.int {
        convention(3)
        aem.prop.int("instance.check.$name.logValuesCount")?.let { set(it) }
    }

    override val status: String get() = statusLogger.entries.firstOrNull()?.summary ?: "Check passed"

    override val success: Boolean get() = statusLogger.entries.none { it.level == LogLevel.ERROR }

    fun <T : Any> state(value: T) = value.also { group.state(it) }

    fun logValues(values: Collection<Any>): String {
        val other = values.size - logValuesCount.get()
        return when {
            other > 0 -> values.take(logValuesCount.get()).joinToString("\n") + "\n... and other ($other)"
            else -> values.take(logValuesCount.get()).joinToString("\n")
        }
    }
}
