package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.http.HttpClient
import com.cognifide.gradle.common.http.ResponseException
import org.apache.http.HttpResponse

@Suppress("MagicNumber")
open class InstanceHttpClient(aem: AemExtension, val instance: Instance) : HttpClient(aem.common) {

    init {
        baseUrl = instance.httpUrl
        basicUser = instance.user
        basicPassword = instance.password
        authorizationPreemptive = true

        connectionTimeout = aem.prop.int("instance.http.connectionTimeout") ?: 30000
        connectionRetries = aem.prop.boolean("instance.http.connectionRetries") ?: true
        connectionIgnoreSsl = aem.prop.boolean("instance.http.connectionIgnoreSsl") ?: true

        proxyHost = aem.prop.string("instance.http.proxyHost")
        proxyPort = aem.prop.int("instance.http.proxyPort")
        proxyScheme = aem.prop.string("instance.http.proxyScheme")
    }

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Instance error. Unexpected response from $instance: ${response.statusLine}")
    }
}
