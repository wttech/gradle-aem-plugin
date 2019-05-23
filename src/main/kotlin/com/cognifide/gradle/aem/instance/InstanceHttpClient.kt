package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpResponse

open class InstanceHttpClient(aem: AemExtension, val instance: Instance) : HttpClient(aem) {

    init {
        baseUrl = instance.httpUrl
        basicUser = instance.user
        basicPassword = instance.password
        authorizationPreemptive = true

        apply(aem.instanceOptions.httpOptions)
    }

    override fun checkStatus(response: HttpResponse, checker: (Int) -> Boolean) {
        if (!checker(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response from $instance: ${response.statusLine}")
        }
    }
}