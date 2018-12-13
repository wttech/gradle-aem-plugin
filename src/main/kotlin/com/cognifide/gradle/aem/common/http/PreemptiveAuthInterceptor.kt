package com.cognifide.gradle.aem.common.http

import org.apache.http.HttpException
import org.apache.http.HttpHost
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.auth.AuthScope
import org.apache.http.auth.AuthState
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.protocol.HttpContext
import org.apache.http.protocol.HttpCoreContext

/**
 * @see <https://stackoverflow.com/a/4328694> due to <https://forums.adobe.com/thread/2389052>
 */
class PreemptiveAuthInterceptor : HttpRequestInterceptor {

    override fun process(request: HttpRequest, context: HttpContext) {
        val authState = context.getAttribute(HttpClientContext.TARGET_AUTH_STATE) as AuthState

        if (authState.authScheme == null) {
            val credsProvider = context.getAttribute(HttpClientContext.CREDS_PROVIDER) as CredentialsProvider
            val targetHost = context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST) as HttpHost
            val creds = credsProvider.getCredentials(AuthScope(targetHost.getHostName(), targetHost.getPort()))
                    ?: throw HttpException("No credentials for preemptive authentication")

            authState.update(BasicScheme(), creds)
        }
    }
}