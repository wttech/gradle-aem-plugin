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
        http.connectionTimeout.convention(1000)
        http.connectionRetries.convention(false)

        applyInstanceInitialized()
    }

    fun sync(callback: InstanceSync.() -> Unit) = sync.using(callback)

    override val status: String get() = statusLogger.entries.firstOrNull()?.summary ?: "Check passed"

    override val success: Boolean get() = statusLogger.entries.none { it.level == LogLevel.ERROR }

    fun <T : Any> state(value: T) = value.also { group.state(it) }

    fun logValues(values: Collection<Any>): String {
        val other = values.size - LOG_VALUES_COUNT
        return when {
            other > 0 -> values.take(LOG_VALUES_COUNT).joinToString("\n") + "\n... and other ($other)"
            else -> values.take(LOG_VALUES_COUNT).joinToString("\n")
        }
    }

    private fun InstanceSync.applyInstanceInitialized() {
        if (!(instance.run { this is LocalInstance && !initialized })) {
            return
        }

        val authInit: Boolean = progress.stateData[STATE_AUTH_INIT] as Boolean? ?: false
        if (authInit) {
            http.basicUser.set(Instance.USER_DEFAULT)
            http.basicPassword.set(Instance.PASSWORD_DEFAULT)
        }

        http.responseHandler { response ->
            if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                val authInitCurrent = progress.stateData[STATE_AUTH_INIT] as Boolean? ?: false
                if (!authInitCurrent) {
                    logger.info("Switching instance '${instance.name}' credentials from customized to defaults.")
                    http.basicUser.set(Instance.USER_DEFAULT)
                    http.basicPassword.set(Instance.PASSWORD_DEFAULT)
                } else {
                    logger.info("Switching instance '${instance.name}' credentials from defaults to customized.")
                    http.basicUser.set(instance.user)
                    http.basicPassword.set(instance.password)
                }
                progress.stateData[STATE_AUTH_INIT] = !authInitCurrent
            }
        }
    }

    companion object {
        const val LOG_VALUES_COUNT = 10

        private const val STATE_AUTH_INIT = "authInit"
    }
}
