package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpResponse
import org.gradle.api.Project

open class InstanceHttpClient(project: Project, val instance: Instance) : HttpClient(project) {

    val aem = AemExtension.of(project)

    init {
        baseUrl = instance.httpUrl
        basicUser = instance.user
        basicPassword = instance.password
        authorizationPreemptive = true

        apply(aem.config.instanceHttpOptions)
    }

    override fun checkStatus(response: HttpResponse, checker: (Int) -> Boolean) {
        if (!checker(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response from $instance: ${response.statusLine}")
        }
    }
}