package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.common.http.HttpClient
import com.cognifide.gradle.common.http.ResponseException
import org.apache.http.HttpResponse

@Suppress("MagicNumber")
open class InstanceHttpClient(sling: SlingExtension, val instance: Instance) : HttpClient(sling.common) {

    init {
        baseUrl.set(instance.httpUrl)
        basicUser.set(instance.user)
        basicPassword.set(instance.password)
        authorizationPreemptive.set(true)

        connectionTimeout.apply {
            convention(30_000)
            sling.prop.int("instance.http.connectionTimeout")?.let { set(it) }
        }
        connectionRetries.apply {
            convention(true)
            sling.prop.boolean("instance.http.connectionRetries")?.let { set(it) }
        }
        connectionIgnoreSsl.apply {
            convention(true)
            sling.prop.boolean("instance.http.connectionIgnoreSsl")?.let { set(it) }
        }
        proxyHost.apply {
            sling.prop.string("instance.http.proxyHost")?.let { set(it) }
        }
        proxyPort.apply {
            sling.prop.int("instance.http.proxyPort")?.let { set(it) }
        }
        proxyScheme.apply {
            sling.prop.string("instance.http.proxyScheme")?.let { set(it) }
        }
    }

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Instance error. Unexpected response from $instance: ${response.statusLine}")
    }
}
