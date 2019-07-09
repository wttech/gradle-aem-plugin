package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.build.BuildScope
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.LocalInstance
import org.apache.http.HttpStatus
import org.gradle.api.logging.LogLevel

abstract class DefaultCheck(protected val group: CheckGroup) : Check {

    val runner = group.runner

    protected val aem = runner.aem

    val instance = group.instance

    val progress: CheckProgress
        get() = runner.progress(instance)

    val statusLogger = group.statusLogger

    var sync: InstanceSync = instance.sync.apply {
        val init = instance.run { this is LocalInstance && !initialized }

        val scope = BuildScope.of(aem.project)
        val authInitKey = "${instance.name}.authInit"
        val authInit = scope.get(authInitKey) ?: false

        http.connectionTimeout = 1000
        http.connectionRetries = false

        if (init && authInit) {
            http.basicUser = Instance.USER_DEFAULT
            http.basicPassword = Instance.PASSWORD_DEFAULT
        }

        http.responseHandler = { response ->
            if (init && response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                if (authInit) {
                    aem.logger.info("Switching instance credentials from customized to defaults.")
                } else {
                    aem.logger.info("Switching instance credentials from defaults to customized.")
                }
                scope.put(authInitKey, !authInit)
            }
        }
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