package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.http.HttpClient
import com.cognifide.gradle.common.http.ResponseException
import org.apache.http.HttpResponse

@Suppress("MagicNumber")
open class InstanceHttpClient(private val aem: AemExtension, val instance: Instance) : HttpClient(aem.common) {

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Instance error. Unexpected response from $instance: ${response.statusLine}")
    }

    init {
        baseUrl.set(instance.httpUrl)
        escapeUrl.set(true)
        authorizationPreemptive.set(true)
        basicCredentials = instance.credentials

        connectionTimeout.apply {
            convention(30_000)
            aem.prop.int("instance.http.connectionTimeout")?.let { set(it) }
        }
        connectionRetries.apply {
            convention(true)
            aem.prop.boolean("instance.http.connectionRetries")?.let { set(it) }
        }
        connectionIgnoreSsl.apply {
            convention(true)
            aem.prop.boolean("instance.http.connectionIgnoreSsl")?.let { set(it) }
        }
        proxyHost.apply {
            aem.prop.string("instance.http.proxyHost")?.let { set(it) }
        }
        proxyPort.apply {
            aem.prop.int("instance.http.proxyPort")?.let { set(it) }
        }
        proxyScheme.apply {
            aem.prop.string("instance.http.proxyScheme")?.let { set(it) }
        }
    }
}
