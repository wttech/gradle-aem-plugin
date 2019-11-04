package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpResponse

@Suppress("MagicNumber")
open class InstanceHttpClient(aem: AemExtension, val instance: Instance) : HttpClient(aem) {

    init {
        baseUrl = instance.httpUrl
        basicUser = instance.user
        basicPassword = instance.password
        authorizationPreemptive = true

        connectionTimeout = aem.props.int("instance.http.connectionTimeout") ?: 30000
        connectionRetries = aem.props.boolean("instance.http.connectionRetries") ?: true
        connectionIgnoreSsl = aem.props.boolean("instance.http.connectionIgnoreSsl") ?: true

        proxyHost = aem.props.string("instance.http.proxyHost")
        proxyPort = aem.props.int("instance.http.proxyPort")
        proxyScheme = aem.props.string("instance.http.proxyScheme")
    }

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Instance error. Unexpected response from $instance: ${response.statusLine}")
    }
}
