package com.cognifide.gradle.aem.common.http

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.file.downloader.HttpFileDownloader
import com.fasterxml.jackson.databind.ObjectMapper
import com.jayway.jsonpath.DocumentContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpResponse
import org.apache.http.NameValuePair
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.CookieSpecs
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

@Suppress("TooManyFunctions")
open class HttpClient(val project: Project) {

    var baseUrl = ""

    var basicUser = ""

    var basicPassword = ""

    var authorizationPreemptive = false

    var connectionTimeout = 30000

    var connectionIgnoreSsl = true

    var connectionRetries = true

    var requestConfigurer: HttpRequestBase.() -> Unit = { }

    var clientBuilder: HttpClientBuilder.() -> Unit = {
        useSystemProperties()

        if (authorizationPreemptive) {
            addInterceptorFirst(PreemptiveAuthInterceptor())
        }

        setDefaultRequestConfig(RequestConfig.custom().apply {
            setCookieSpec(CookieSpecs.STANDARD)

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

        if (connectionIgnoreSsl) {
            setSSLSocketFactory(SSLConnectionSocketFactory(SSLContextBuilder()
                    .loadTrustMaterial(null) { _, _ -> true }
                    .build(), NoopHostnameVerifier.INSTANCE))
        }
        if (!connectionRetries) {
            disableAutomaticRetries()
        }
    }

    var responseHandler: (HttpResponse) -> Unit = { }

    var responseChecks: Boolean = true

    var responseChecker: (HttpResponse) -> Unit = { checkStatus(it) }

    fun get(uri: String) = get(uri) { checkStatus(it) }

    fun <T> get(uri: String, handler: HttpClient.(HttpResponse) -> T): T {
        return execute(getMethod(uri), handler)
    }

    fun getMethod(uri: String) = HttpGet(baseUrl(uri))

    fun head(uri: String) = head(uri) { checkStatus(it) }

    fun <T> head(uri: String, handler: HttpClient.(HttpResponse) -> T): T {
        return execute(headMethod(uri), handler)
    }

    fun headMethod(uri: String) = HttpHead(baseUrl(uri))

    fun delete(uri: String) = delete(uri) { checkStatus(it) }

    fun <T> delete(uri: String, handler: HttpClient.(HttpResponse) -> T): T {
        return execute(deleteMethod(uri), handler)
    }

    fun deleteMethod(uri: String) = HttpDelete(baseUrl(uri))

    fun put(uri: String) = put(uri) { checkStatus(it) }

    fun <T> put(uri: String, handler: HttpClient.(HttpResponse) -> T): T {
        return execute(putMethod(uri), handler)
    }

    fun putMethod(uri: String) = HttpPut(baseUrl(uri))

    fun patch(path: String) = patch(path) { checkStatus(it) }

    fun <T> patch(uri: String, handler: HttpClient.(HttpResponse) -> T): T {
        return execute(patchMethod(uri), handler)
    }

    fun patchMethod(uri: String): HttpPatch = HttpPatch(baseUrl(uri))

    fun post(url: String, params: Map<String, Any> = mapOf()) = postUrlencoded(url, params)

    fun <T> post(uri: String, params: Map<String, Any> = mapOf(), handler: HttpClient.(HttpResponse) -> T): T {
        return postUrlencoded(uri, params, handler)
    }

    fun postUrlencoded(uri: String, params: Map<String, Any> = mapOf()) = postUrlencoded(uri, params) { checkStatus(it) }

    fun <T> postUrlencoded(uri: String, params: Map<String, Any> = mapOf(), handler: HttpClient.(HttpResponse) -> T): T {
        return post(uri, createEntityUrlencoded(params), handler)
    }

    fun postMultipart(uri: String, params: Map<String, Any> = mapOf()) = postMultipart(uri, params) { checkStatus(it) }

    fun <T> postMultipart(uri: String, params: Map<String, Any> = mapOf(), handler: HttpClient.(HttpResponse) -> T): T {
        return post(uri, createEntityMultipart(params), handler)
    }

    fun <T> post(uri: String, entity: HttpEntity, handler: HttpClient.(HttpResponse) -> T): T {
        return execute(postMethod(uri).apply { this.entity = entity }, handler)
    }

    fun postMethod(uri: String) = HttpPost(baseUrl(uri))

    fun asStream(response: HttpResponse): InputStream {
        if (responseChecks) {
            responseChecker(response)
        }

        return response.entity.content
    }

    fun asJson(response: HttpResponse): DocumentContext {
        return Formats.asJson(asStream(response))
    }

    fun asString(response: HttpResponse): String {
        return IOUtils.toString(asStream(response), Charsets.UTF_8) ?: ""
    }

    fun <T> asObjectFromJson(response: HttpResponse, clazz: Class<T>): T {
        return try {
            ObjectMapper().readValue(asStream(response), clazz)
        } catch (e: IOException) {
            throw ResponseException("Cannot parse / malformed response: $response")
        }
    }

    fun checkStatus(response: HttpResponse, statusCode: Int) = checkStatus(response, listOf(statusCode))

    fun checkStatus(response: HttpResponse, statusCodes: List<Int>) = checkStatus(response) { statusCodes.contains(it) }

    open fun checkStatus(response: HttpResponse, checker: (Int) -> Boolean = { it in STATUS_CODE_VALID }) {
        if (!checker(response.statusLine.statusCode)) {
            throw ResponseException("Unexpected response: ${response.statusLine}")
        }
    }

    /**
     * Fix for HttpClient's: 'escaped absolute path not valid'
     * https://stackoverflow.com/questions/13652681/httpclient-invalid-uri-escaped-absolute-path-not-valid
     */
    open fun baseUrl(uri: String): String {
            return "$baseUrl${uri.replace(" ", "%20")}"
    }

    @Suppress("TooGenericExceptionCaught")
    open fun <T> execute(method: HttpRequestBase, handler: HttpClient.(HttpResponse) -> T): T {
        try {
            requestConfigurer(method)

            val client = HttpClientBuilder.create().apply(clientBuilder).build()
            val response = client.execute(method)

            responseHandler(response)

            return handler.invoke(this, response)
        } catch (e: Exception) {
            throw RequestException("Failed request to $method: ${e.message}", e)
        } finally {
            method.releaseConnection()
        }
    }

    fun execute(method: HttpRequestBase) = execute(method) { checkStatus(it) }

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

    fun download(path: String, target: File) = HttpFileDownloader(project, this).download(path, target)

    fun downloadTo(path: String, dir: File) = File(dir, path.substringAfterLast("/")).apply { download(path, this) }

    companion object {
        val STATUS_CODE_VALID = 200 until 300
    }
}