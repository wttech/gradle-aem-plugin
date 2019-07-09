package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.BuildScope
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus

open class InstanceHttpClient(aem: AemExtension, val instance: Instance) : HttpClient(aem) {

    init {
        baseUrl = instance.httpUrl
        basicUser = instance.user
        basicPassword = instance.password
        authorizationPreemptive = true

        if (instance.run { this is LocalInstance && !initialized }) {
            val buildScope = BuildScope.of(aem.project)
            val authInitKey = "${instance.name}.authInit"
            val authInit = buildScope.get(authInitKey) ?: false

            if (authInit) {
                basicUser = Instance.USER_DEFAULT
                basicPassword = Instance.PASSWORD_DEFAULT
            }

            responseHandler = { response ->
                if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                    if (authInit) {
                        aem.logger.info("Switching instance credentials from customized to defaults.")
                    } else {
                        aem.logger.info("Switching instance credentials from defaults to customized.")
                    }
                    buildScope.put(authInitKey, !authInit)
                }
            }
        }

        apply { aem.instanceOptions.httpOptions(this, instance) }
    }

    override fun checkStatus(response: HttpResponse, checker: (Int) -> Boolean) {
        if (!checker(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response from $instance: ${response.statusLine}")
        }
    }
}