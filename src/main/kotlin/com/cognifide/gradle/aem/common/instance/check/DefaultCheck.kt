package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.LocalInstance
import com.cognifide.gradle.common.utils.using
import org.apache.http.HttpStatus
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
        toggleBasicCredentialsWhenInitialized()
    }

    fun sync(callback: InstanceSync.() -> Unit) = sync.using(callback)

    val name get() = this::class.simpleName!!.removeSuffix("Check").decapitalize()

    override val enabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("instance.check.$name.enabled")?.let { set(it) }
    }

    val logValuesCount = aem.obj.int {
        convention(5)
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

    private fun InstanceSync.toggleBasicCredentialsWhenInitialized() {
        if (instance !is LocalInstance || instance.initialized) {
            return
        }

        val authInit: Boolean = progress.stateData[STATE_AUTH_INIT] as Boolean? ?: true
        if (authInit) {
            http.basicCredentials = Instance.CREDENTIALS_DEFAULT
        }

        http.responseHandler { response ->
            if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                val authInitCurrent = progress.stateData[STATE_AUTH_INIT] as Boolean? ?: true
                if (authInitCurrent) {
                    logger.info("Switching instance '${instance.name}' credentials from customized to defaults.")
                    http.basicCredentials = Instance.CREDENTIALS_DEFAULT
                } else {
                    logger.info("Switching instance '${instance.name}' credentials from defaults to customized.")
                    http.basicCredentials = instance.credentials
                }
                progress.stateData[STATE_AUTH_INIT] = !authInitCurrent
            }
        }
    }

    companion object {
        private const val STATE_AUTH_INIT = "authInit"
    }
}
