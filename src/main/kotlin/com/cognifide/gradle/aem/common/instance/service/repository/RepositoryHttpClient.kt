package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceHttpClient
import org.apache.http.HttpResponse

class RepositoryHttpClient(aem: AemExtension, instance: Instance) : InstanceHttpClient(aem, instance) {

    override fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Repository error. Unexpected response from $instance: ${response.statusLine}\n" +
                response.entity.content.bufferedReader().readText())
    }
}