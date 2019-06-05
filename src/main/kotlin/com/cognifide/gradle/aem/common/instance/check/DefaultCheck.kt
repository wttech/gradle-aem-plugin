package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceSync
import com.cognifide.gradle.aem.common.instance.isBeingInitialized
import org.apache.http.HttpStatus
import org.gradle.api.logging.LogLevel

abstract class DefaultCheck(protected val group: CheckGroup) : Check {

    val runner = group.runner

    protected val aem = runner.aem

    val instance = group.instance

    val statusLogger = group.statusLogger

    var sync: InstanceSync = instance.sync.apply {
        val init = instance.isBeingInitialized()

        http.connectionTimeout = 1000
        http.connectionRetries = false

        if (init) {
            aem.logger.debug("Initializing instance using default credentials.")
            http.basicUser = Instance.USER_DEFAULT
            http.basicPassword = Instance.PASSWORD_DEFAULT
        }

        http.responseHandler = { response ->
            if (init && response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                if (http.basicUser == Instance.USER_DEFAULT) {
                    aem.logger.debug("Switching instance credentials from defaults to customized.")
                    http.basicUser = instance.user
                    http.basicPassword = instance.password
                } else {
                    aem.logger.debug("Switching instance credentials from customized to defaults.")
                    http.basicUser = Instance.USER_DEFAULT
                    http.basicPassword = Instance.PASSWORD_DEFAULT
                }
            }
        }
    }

    override val status: String
        get() = statusLogger.entries.firstOrNull()?.summary ?: "Check passed"

    override val success: Boolean
        get() = statusLogger.entries.none { it.level == LogLevel.ERROR }

    fun <T : Any> state(value: T) = value.also { group.state(it) }

    val stateTime: Long
        get() = runner.stateTime(instance)

    val stateChanged: Boolean
        get() = runner.stateChanged(instance)
}