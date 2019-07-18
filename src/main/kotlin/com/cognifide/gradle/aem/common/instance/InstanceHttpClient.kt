package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpResponse

open class InstanceHttpClient(aem: AemExtension, val instance: Instance) : HttpClient(aem) {

    init {
        baseUrl = instance.httpUrl
        basicUser = instance.user
        basicPassword = instance.password
        authorizationPreemptive = true

        apply { aem.instanceOptions.httpOptions(this, instance) }
    }

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Instance error. Unexpected response from $instance: ${response.statusLine}")
    }
}