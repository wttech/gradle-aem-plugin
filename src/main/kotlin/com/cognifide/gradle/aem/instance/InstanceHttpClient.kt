package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.internal.http.PreemptiveAuthInterceptor
import com.cognifide.gradle.aem.pkg.DeployException
import com.cognifide.gradle.aem.pkg.RequestException
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.HttpStatus
import org.apache.http.NameValuePair
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.entity.UrlEncodedFormEntity
import org.apache.http.client.methods.*
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicNameValuePair
import org.apache.http.ssl.SSLContextBuilder
import org.gradle.api.Project
import java.io.File
import java.io.InputStream
import java.util.*

open class InstanceHttpClient(val project: Project, val instance: Instance) {

    val aem = AemExtension.of(project)

    var basicUser = instance.user

    var basicPassword = instance.password

    var connectionTimeout = aem.config.instanceConnectionTimeout

    var connectionUntrustedSsl = aem.config.instanceConnectionUntrustedSsl

    var connectionRetries = true

    var requestConfigurer: (HttpRequestBase) -> Unit = { }

    var responseHandler: (HttpResponse) -> Unit = { }

    fun get(path: String) = get(path) {}

    fun <T> get(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpGet(composeUrl(path)), handler)
    }

    fun head(path: String) = head(path) {}

    fun <T> head(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpHead(composeUrl(path)), handler)
    }

    fun delete(path: String) = delete(path) {}

    fun <T> delete(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpDelete(composeUrl(path)), handler)
    }

    fun put(path: String) = put(path) {}

    fun <T> put(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpPut(composeUrl(path)), handler)
    }

    fun patch(path: String) = patch(path) {}

    fun <T> patch(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpPatch(composeUrl(path)), handler)
    }

    fun post(url: String, params: Map<String, Any> = mapOf()) = postUrlencoded(url, params)

    fun <T> post(url: String, params: Map<String, Any> = mapOf(), handler: (HttpResponse) -> T): T = postUrlencoded(url, params, handler)

    fun postUrlencoded(url: String, params: Map<String, Any> = mapOf()) = postUrlencoded(url, params) {}

    fun <T> postUrlencoded(url: String, params: Map<String, Any> = mapOf(), handler: (HttpResponse) -> T): T {
        return post(url, createEntityUrlencoded(params), handler)
    }

    fun postMultipart(url: String, params: Map<String, Any> = mapOf()) = postMultipart(url, params) {}

    fun <T> postMultipart(url: String, params: Map<String, Any> = mapOf(), handler: (HttpResponse) -> T): T {
        return post(url, createEntityMultipart(params), handler)
    }

    private fun <T> post(url: String, entity: HttpEntity, handler: (HttpResponse) -> T): T {
        return execute(HttpPost(composeUrl(url)).apply { this.entity = entity }, handler)
    }

    fun checkStatus(response: HttpResponse, status: Int = HttpStatus.SC_OK) = checkStatus(response, listOf(status))

    fun checkStatus(response: HttpResponse, statuses: Collection<Int>) {
        if (!statuses.contains(response.statusLine.statusCode)) {
            throw DeployException("Unexpected response from $instance: ${response.statusLine}")
        }
    }

    fun asString(response: HttpResponse): String {
        checkStatus(response)

        return IOUtils.toString(response.entity.content) ?: ""
    }

    fun asStream(response: HttpResponse): InputStream {
        checkStatus(response)

        return response.entity.content
    }

    /**
     * Fix for HttpClient's: 'escaped absolute path not valid'
     * https://stackoverflow.com/questions/13652681/httpclient-invalid-uri-escaped-absolute-path-not-valid
     */
    private fun composeUrl(url: String): String {
        return "${instance.httpUrl}${url.replace(" ", "%20")}"
    }

    private fun <T> execute(method: HttpRequestBase, handler: (HttpResponse) -> T): T {
        try {
            requestConfigurer(method)

            val client = createHttpClient()
            val response = client.execute(method)

            responseHandler(response)

            return handler(response)
        } catch (e: Exception) {
            throw RequestException("Failed request to $instance: ${e.message}", e)
        } finally {
            method.releaseConnection()
        }
    }

    fun createHttpClient(): HttpClient {
        val builder = HttpClientBuilder.create()
                .addInterceptorFirst(PreemptiveAuthInterceptor())
                .setDefaultRequestConfig(RequestConfig.custom().apply {
                    if (!connectionRetries) {
                        setSocketTimeout(connectionTimeout)
                    }
                    setConnectTimeout(connectionTimeout)
                    setConnectionRequestTimeout(connectionTimeout)
                }.build())
                .setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, UsernamePasswordCredentials(basicUser, basicPassword))
                })
        if (connectionUntrustedSsl) {
            builder.setSSLSocketFactory(SSLConnectionSocketFactory(SSLContextBuilder()
                    .loadTrustMaterial(null) { _, _ -> true }
                    .build(), NoopHostnameVerifier.INSTANCE))
        }
        if (!connectionRetries) {
            builder.disableAutomaticRetries()
        }

        return builder.build()
    }

    private fun createEntityUrlencoded(params: Map<String, Any>): HttpEntity {
        return UrlEncodedFormEntity(params.entries.fold(ArrayList<NameValuePair>()) { result, e ->
            result.add(BasicNameValuePair(e.key, e.value.toString())); result
        })
    }

    private fun createEntityMultipart(params: Map<String, Any>): HttpEntity {
        val builder = MultipartEntityBuilder.create()
        for ((key, value) in params) {
            if (value is File) {
                if (value.exists()) {
                    builder.addBinaryBody(key, value)
                }
            } else {
                val str = value.toString()
                if (str.isNotBlank()) {
                    builder.addTextBody(key, str)
                }
            }
        }

        return builder.build()
    }

}