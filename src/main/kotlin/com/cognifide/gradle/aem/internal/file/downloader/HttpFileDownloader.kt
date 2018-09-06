package com.cognifide.gradle.aem.internal.file.downloader

import com.cognifide.gradle.aem.internal.file.FileException
import com.cognifide.gradle.aem.internal.http.PreemptiveAuthInterceptor
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.HttpClient
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.client.LaxRedirectStrategy
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class HttpFileDownloader(val project: Project) {

    var username: String? = null

    var password: String? = null

    var ignoreSSLErrors: Boolean = true

    val logger: Logger = project.logger

    companion object {
        val PROTOCOLS_HANDLED = arrayOf("http://", "https://")

        val STATUS_CODES_VALID = 200..300

        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && (PROTOCOLS_HANDLED.any { sourceUrl.startsWith(it) })
        }
    }

    fun download(sourceUrl: String, targetFile: File) {
        try {
            val client = createClient()
            val response = client.execute(HttpGet(sourceUrl))
            val statusCode = response.statusLine.statusCode
            if (statusCode !in STATUS_CODES_VALID) {
                throw IllegalStateException("Cannot download file from URL '$sourceUrl' due to invalid status code of response ($statusCode).")
            }

            val downloader = ProgressFileDownloader(project)
            downloader.headerSourceTarget(sourceUrl, targetFile)
            downloader.size = response.entity.contentLength

            downloader.download(response.entity.content, targetFile)
        } catch (e: Exception) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using HTTP(s). Check connection.", e)
        }
    }

    private fun createClient(): HttpClient {
        val builder = HttpClients.custom()
                .addInterceptorFirst(PreemptiveAuthInterceptor())
                .useSystemProperties()
                .setDefaultRequestConfig(
                        RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()
                )

        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            val provider = BasicCredentialsProvider()
            provider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
            builder.setDefaultCredentialsProvider(provider)
        }

        if (ignoreSSLErrors) {
            builder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
        }

        builder.setRedirectStrategy(LaxRedirectStrategy())

        return builder.build()
    }

}