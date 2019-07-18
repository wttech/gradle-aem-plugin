package com.cognifide.gradle.aem.common.http

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.common.utils.Utils
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.jayway.jsonpath.DocumentContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.Serializable
import org.apache.commons.io.IOUtils
import org.apache.http.HttpEntity
import org.apache.http.HttpHost
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

@Suppress("TooManyFunctions")
open class HttpClient(private val aem: AemExtension) : Serializable {

    private val project = aem.project

    var connectionTimeout = 30000

    var connectionIgnoreSsl = true

    var connectionRetries = true

    var authorizationPreemptive = false

    var baseUrl = ""

    var basicUser: String? = null

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var basicPassword: String? = null

    var proxyHost: String? = null

    var proxyPort: Int? = null

    var proxyScheme: String? = null

    @JsonIgnore
    var requestConfigurer: HttpRequestBase.() -> Unit = { }

    @JsonIgnore
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

        if (!proxyHost.isNullOrBlank() && proxyPort != null) {
            setProxy(HttpHost(proxyHost, proxyPort!!, proxyScheme))
        }

        if (!basicUser.isNullOrBlank() && !basicPassword.isNullOrBlank()) {
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

    @JsonIgnore
    var responseHandler: (HttpResponse) -> Unit = { }

    var responseChecks: Boolean = true

    @JsonIgnore
    var responseChecker: (HttpResponse) -> Unit = { checkStatus(it) }

    fun <T> request(method: String, uri: String, handler: HttpClient.(HttpResponse) -> T) = when (method.toLowerCase()) {
        "get" -> get(uri, handler)
        "post" -> post(uri, handler)
        "put" -> put(uri, handler)
        "patch" -> patch(uri, handler)
        "head" -> head(uri, handler)
        "delete" -> delete(uri, handler)
        else -> throw RequestException("Invalid HTTP client method: '$method'")
    }

    fun get(uri: String) = get(uri) { checkStatus(it) }

    fun <T> get(uri: String, handler: HttpClient.(HttpResponse) -> T): T = get(uri, handler) {}

    fun <T> get(uri: String, handler: HttpClient.(HttpResponse) -> T, options: HttpGet.() -> Unit): T {
        return execute(HttpGet(baseUrl(uri)).apply(options), handler)
    }

    fun head(uri: String) = head(uri) { checkStatus(it) }

    fun <T> head(uri: String, handler: HttpClient.(HttpResponse) -> T): T = head(uri, handler)

    fun <T> head(uri: String, handler: HttpClient.(HttpResponse) -> T, options: HttpHead.() -> Unit): T {
        return execute(HttpHead(baseUrl(uri)).apply(options), handler)
    }

    fun delete(uri: String) = delete(uri) { checkStatus(it) }

    fun <T> delete(uri: String, handler: HttpClient.(HttpResponse) -> T): T = delete(uri, handler) {}

    fun <T> delete(uri: String, handler: HttpClient.(HttpResponse) -> T, options: HttpDelete.() -> Unit): T {
        return execute(HttpDelete(baseUrl(uri)).apply(options), handler)
    }

    fun put(uri: String) = put(uri) { checkStatus(it) }

    fun <T> put(uri: String, handler: HttpClient.(HttpResponse) -> T): T = put(uri, handler) {}

    fun <T> put(uri: String, handler: HttpClient.(HttpResponse) -> T, options: HttpPut.() -> Unit): T {
        return execute(HttpPut(baseUrl(uri)).apply(options), handler)
    }

    fun patch(path: String) = patch(path) { checkStatus(it) }

    fun <T> patch(uri: String, handler: HttpClient.(HttpResponse) -> T): T = patch(uri, handler) {}

    fun <T> patch(uri: String, handler: HttpClient.(HttpResponse) -> T, options: HttpPatch.() -> Unit): T {
        return execute(HttpPatch(baseUrl(uri)).apply(options), handler)
    }

    fun post(url: String, params: Map<String, Any?> = mapOf()) = postUrlencoded(url, params)

    fun <T> post(uri: String, params: Map<String, Any> = mapOf(), handler: HttpClient.(HttpResponse) -> T): T {
        return postUrlencoded(uri, params, handler)
    }

    fun postUrlencoded(uri: String, params: Map<String, Any?> = mapOf()) = postUrlencoded(uri, params) { checkStatus(it) }

    fun <T> postUrlencoded(uri: String, params: Map<String, Any?> = mapOf(), handler: HttpClient.(HttpResponse) -> T): T {
        return post(uri, handler) { entity = createEntityUrlencoded(params) }
    }

    fun postMultipart(uri: String, params: Map<String, Any?> = mapOf()) = postMultipart(uri, params) { checkStatus(it) }

    fun <T> postMultipart(uri: String, params: Map<String, Any?> = mapOf(), handler: HttpClient.(HttpResponse) -> T): T {
        return post(uri, handler) { entity = createEntityMultipart(params) }
    }

    fun <T> post(uri: String, handler: HttpClient.(HttpResponse) -> T): T = post(uri, handler) {}

    fun <T> post(uri: String, handler: HttpClient.(HttpResponse) -> T, options: HttpPost.() -> Unit): T {
        return execute(HttpPost(baseUrl(uri)).apply(options), handler)
    }

    fun asStream(response: HttpResponse): InputStream {
        if (responseChecks) {
            responseChecker(response)
        }

        return response.entity.content
    }

    fun asJson(response: HttpResponse): DocumentContext {
        return Formats.asJson(asStream(response))
    }

    fun asJson(jsonString: String): DocumentContext {
        return Formats.asJson(jsonString)
    }

    fun asString(response: HttpResponse): String {
        return IOUtils.toString(asStream(response), Charsets.UTF_8) ?: ""
    }

    fun <T> asObjectFromJson(response: HttpResponse, clazz: Class<T>): T {
        return try {
            ObjectMapper().readValue(asStream(response), clazz)
        } catch (e: IOException) {
            throw ResponseException("Cannot parse / malformed response: $response", e)
        }
    }

    fun checkStatus(response: HttpResponse, statusCodes: IntRange = STATUS_CODE_VALID) {
        if (response.statusLine.statusCode !in statusCodes) {
            throwStatusException(response)
        }
    }

    fun checkStatus(response: HttpResponse, statusCode: Int) = checkStatus(response, listOf(statusCode))

    fun checkStatus(response: HttpResponse, statusCodes: List<Int>) {
        if (!statusCodes.contains(response.statusLine.statusCode)) {
            throwStatusException(response)
        }
    }

    open fun throwStatusException(response: HttpResponse) {
        throw ResponseException("Unexpected response detected: ${response.statusLine}")
    }

    fun checkText(response: HttpResponse, containedText: String, ignoreCase: Boolean = true) {
        val text = asString(response)
        if (!text.contains(containedText, ignoreCase)) {
            aem.logger.debug("Actual text:\n$text")
            throw ResponseException("Response does not contain text: $containedText")
        }
    }

    fun checkHtml(response: HttpResponse, validator: Document.() -> Boolean) {
        val html = asString(response)
        val doc = Jsoup.parse(html)
        if (!validator(doc)) {
            aem.logger.debug("Actual HTML:\n$html")
            throw ResponseException("Response HTML does not pass validation")
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

    open fun createEntityUrlencoded(params: Map<String, Any?>): HttpEntity {
        return UrlEncodedFormEntity(params.entries.fold(mutableListOf<NameValuePair>()) { result, (key, value) ->
            Utils.unroll(value) { addEntityUrlencoded(result, key, it) }
            result
        })
    }

    private fun addEntityUrlencoded(result: MutableList<NameValuePair>, key: String, value: Any?) {
        result.add(BasicNameValuePair(key, value?.toString() ?: ""))
    }

    open fun createEntityMultipart(params: Map<String, Any?>): HttpEntity {
        return MultipartEntityBuilder.create().apply {
            params.forEach { (key, value) -> Utils.unroll(value) { addEntityMultipart(key, it) } }
        }.build()
    }

    private fun MultipartEntityBuilder.addEntityMultipart(key: String, value: Any?) {
        if (value is File && value.exists()) {
            addBinaryBody(key, value)
        } else {
            addTextBody(key, value?.toString() ?: "")
        }
    }

    fun <T> fileTransfer(operation: HttpFileTransfer.() -> T): T = aem.httpFile { client = this@HttpClient; operation() }

    companion object {
        val STATUS_CODE_VALID = 200 until 300
    }
}