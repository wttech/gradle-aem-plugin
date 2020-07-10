package com.cognifide.gradle.sling.common.instance.service.repository

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.InstanceHttpClient
import com.cognifide.gradle.common.http.ResponseException
import org.apache.http.HttpResponse

class RepositoryHttpClient(sling: SlingExtension, instance: Instance) : InstanceHttpClient(sling, instance) {

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Repository error. Unexpected response from $instance: ${response.statusLine}\n" +
                response.entity.content.bufferedReader().readText())
    }
}
