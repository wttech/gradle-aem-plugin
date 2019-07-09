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
        authorizationPreemptive = true

        basicUser = instance.user
        basicPassword = instance.password

        applyAuthOnInit(aem)
        apply { aem.instanceOptions.httpOptions(this, instance) }
    }

    private fun applyAuthOnInit(aem: AemExtension) {
        if (!(instance.run { this is LocalInstance && !initialized })) {
            return
        }

        val cache = BuildScope.of(aem.project)
        val cacheKey = "${instance.name}.authOnInit"

        val authInit = cache.get(cacheKey) ?: false
        if (authInit) {
            basicUser = Instance.USER_DEFAULT
            basicPassword = Instance.PASSWORD_DEFAULT
        }

        responseHandler = { response ->
            if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                val authInitCurrent = cache.get(cacheKey) ?: false
                if (authInitCurrent) {
                    aem.logger.info("Switching instance credentials from customized to defaults.")

                    basicUser = Instance.USER_DEFAULT
                    basicPassword = Instance.PASSWORD_DEFAULT
                } else {
                    aem.logger.info("Switching instance credentials from defaults to customized.")

                    basicUser = instance.user
                    basicPassword = instance.password
                }

                cache.put(cacheKey, !authInitCurrent)
            }
        }
    }

    override fun checkStatus(response: HttpResponse, checker: (Int) -> Boolean) {
        if (!checker(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response from $instance: ${response.statusLine}")
        }
    }
}