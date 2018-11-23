package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.internal.http.HttpClient
import com.cognifide.gradle.aem.internal.http.ResponseException
import org.apache.http.HttpResponse
import org.gradle.api.Project

open class InstanceHttpClient(project: Project, val instance: Instance) : HttpClient(project) {

    val aem = AemExtension.of(project)

    init {
        basicUser = instance.user
        basicPassword = instance.password

        apply(aem.config.instanceHttpOptions)
    }

    override fun baseUrl(url: String): String {
        return "${instance.httpUrl}${url.replace(" ", "%20")}"
    }

    override fun checkStatus(response: HttpResponse, statuses: Collection<Int>) {
        if (!statuses.contains(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response from $instance: ${response.statusLine}")
        }
    }
}