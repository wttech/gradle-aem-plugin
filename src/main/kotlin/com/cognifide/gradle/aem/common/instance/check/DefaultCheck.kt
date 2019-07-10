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
        http.connectionTimeout = 1000
        http.connectionRetries = false

        applyInstanceInitialized()
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

    private fun InstanceSync.applyInstanceInitialized() {
        if (!(instance.run { this is LocalInstance && !initialized })) {
            return
        }

        val cache = BuildScope.of(aem.project)
        val cacheKey = "instance.${instance.name}.authInit"

        val authInit = cache.get(cacheKey) ?: false
        if (authInit) {
            http.basicUser = Instance.USER_DEFAULT
            http.basicPassword = Instance.PASSWORD_DEFAULT
        }

        val originHttpResponseHandler = http.responseHandler

        http.responseHandler = { response ->
            originHttpResponseHandler(response)

            if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                val authInitCurrent = cache.get(cacheKey) ?: false
                if (!authInitCurrent) {
                    aem.logger.info("Switching instance '${instance.name}' credentials from customized to defaults.")
                    http.basicUser = Instance.USER_DEFAULT
                    http.basicPassword = Instance.PASSWORD_DEFAULT
                } else {
                    aem.logger.info("Switching instance '${instance.name}' credentials from defaults to customized.")
                    http.basicUser = instance.user
                    http.basicPassword = instance.password
                }
                cache.put(cacheKey, !authInitCurrent)
            }
        }
    }

    companion object {
        const val LOG_VALUES_COUNT = 10
    }
}