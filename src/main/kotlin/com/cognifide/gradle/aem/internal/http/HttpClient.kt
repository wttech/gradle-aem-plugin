package com.cognifide.gradle.aem.internal.http

import com.jayway.jsonpath.DocumentContext
import com.jayway.jsonpath.JsonPath
import java.io.File
import java.io.InputStream
import java.util.*
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

open class HttpClient(val project: Project) {

    var basicUser = ""

    var basicPassword = ""

    var connectionTimeout = 30000

    var connectionUntrustedSsl = true

    var connectionRetries = true

    var requestConfigurer: (HttpRequestBase) -> Unit = { }

    var clientBuilder: ((HttpClientBuilder) -> HttpClient) = {
        it.run {
            addInterceptorFirst(PreemptiveAuthInterceptor())

            setDefaultRequestConfig(RequestConfig.custom().apply {
                if (!connectionRetries) {
                    setSocketTimeout(connectionTimeout)
                }
                setConnectTimeout(connectionTimeout)
                setConnectionRequestTimeout(connectionTimeout)
            }.build())

            if (basicUser.isNotBlank() && basicPassword.isNotBlank()) {
                setDefaultCredentialsProvider(BasicCredentialsProvider().apply {
                    setCredentials(AuthScope.ANY, UsernamePasswordCredentials(basicUser, basicPassword))
                })
            }

            if (connectionUntrustedSsl) {
                setSSLSocketFactory(SSLConnectionSocketFactory(SSLContextBuilder()
                        .loadTrustMaterial(null) { _, _ -> true }
                        .build(), NoopHostnameVerifier.INSTANCE))
            }
            if (!connectionRetries) {
                disableAutomaticRetries()
            }

            build()
        }
    }

    var responseHandler: (HttpResponse) -> Unit = { }

    var responseChecks: Boolean = true

    var responseChecker: (HttpResponse) -> Unit = { checkStatus(it) }

    fun get(path: String) = get(path) {}

    fun <T> get(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpGet(baseUrl(path)), handler)
    }

    fun head(path: String) = head(path) {}

    fun <T> head(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpHead(baseUrl(path)), handler)
    }

    fun delete(path: String) = delete(path) {}

    fun <T> delete(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpDelete(baseUrl(path)), handler)
    }

    fun put(path: String) = put(path) {}

    fun <T> put(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpPut(baseUrl(path)), handler)
    }

    fun patch(path: String) = patch(path) {}

    fun <T> patch(path: String, handler: (HttpResponse) -> T): T {
        return execute(HttpPatch(baseUrl(path)), handler)
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

    fun <T> post(url: String, entity: HttpEntity, handler: (HttpResponse) -> T): T {
        return execute(HttpPost(baseUrl(url)).apply { this.entity = entity }, handler)
    }

    fun asStream(response: HttpResponse): InputStream {
        if (responseChecks) {
            responseChecker(response)
        }

        return response.entity.content
    }

    fun asJson(response: HttpResponse): DocumentContext {
        return JsonPath.parse(asStream(response))
    }

    fun asString(response: HttpResponse): String {
        return IOUtils.toString(asStream(response), Charsets.UTF_8) ?: ""
    }

    open fun checkStatus(response: HttpResponse, statuses: Collection<Int> = listOf(HttpStatus.SC_OK)) {
        if (!statuses.contains(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response: ${response.statusLine}")
        }
    }

    /**
     * Fix for HttpClient's: 'escaped absolute path not valid'
     * https://stackoverflow.com/questions/13652681/httpclient-invalid-uri-escaped-absolute-path-not-valid
     */
    open fun baseUrl(url: String): String {
        return url.replace(" ", "%20")
    }

    open fun <T> execute(method: HttpRequestBase, handler: (HttpResponse) -> T): T {
        try {
            requestConfigurer(method)

            val client = clientBuilder(HttpClientBuilder.create())
            val response = client.execute(method)

            responseHandler(response)

            return handler(response)
        } catch (e: Exception) {
            throw RequestException("Failed request to $method: ${e.message}", e)
        } finally {
            method.releaseConnection()
        }
    }

    open fun createEntityUrlencoded(params: Map<String, Any>): HttpEntity {
        return UrlEncodedFormEntity(params.entries.fold(ArrayList<NameValuePair>()) { result, e ->
            result.add(BasicNameValuePair(e.key, e.value.toString())); result
        })
    }

    open fun createEntityMultipart(params: Map<String, Any>): HttpEntity {
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