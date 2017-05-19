package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.multipart.*
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

class DeploySynchronizer(val instance: AemInstance, val config: AemConfig) {

    companion object {
        private val LOG = LoggerFactory.getLogger(DeploySynchronizer::class.java)

        private val PACKAGE_MAMAGER_SERVICE_SUFFIX = "/crx/packmgr/service"

        private val PACKAGE_MANAGER_LIST_SUFFIX = "/crx/packmgr/list.jsp"
    }

    val jsonTargetUrl = instance.url + PACKAGE_MAMAGER_SERVICE_SUFFIX + "/.json"

    val htmlTargetUrl = instance.url + PACKAGE_MAMAGER_SERVICE_SUFFIX + "/.html"

    val listPackagesUrl = instance.url + PACKAGE_MANAGER_LIST_SUFFIX

    fun post(url: String, parts: List<Part> = listOf()): String {
        val method = PostMethod(url)

        try {
            method.requestEntity = MultipartRequestEntity(parts.toTypedArray(), method.params)

            val status = createHttpClient(instance.user, instance.password).executeMethod(method)
            if (status == HttpStatus.SC_OK) {
                return IOUtils.toString(method.responseBodyAsStream)
            } else {
                LOG.warn(method.responseBodyAsString)
                throw DeployException("Request to the repository failed, cause: "
                        + HttpStatus.getStatusText(status) + " (check URL, user and password)")
            }

        } catch (e: IOException) {
            throw DeployException("Request to the repository failed, cause: " + e.message, e)
        } finally {
            method.releaseConnection()
        }
    }

    fun post(url: String, params: Map<String, Any>) = post(url, createParts(params))

    private fun createHttpClient(user: String, password: String): HttpClient {
        val client = HttpClient()
        client.httpConnectionManager.params.connectionTimeout = config.deployConnectionTimeout
        client.params.isAuthenticationPreemptive = true
        client.state.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(user, password))

        return client
    }

    private fun createParts(params: Map<String, Any>): List<Part> {
        val partList = mutableListOf<Part>()
        for ((key, value) in params) {
            if (value is File) {
                val file = value
                try {
                    partList.add(FilePart(key, FilePartSource(file.name, file)))
                } catch (e: FileNotFoundException) {
                    throw DeployException(String.format("Upload param '%s' has invalid file specified.", key), e)
                }
            } else {
                val str = value.toString()
                if (!str.isNullOrBlank()) {
                    partList.add(StringPart(key, str))
                }
            }
        }

        return partList
    }

}