package com.cognifide.gradle.aem.environment.checks

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.ProgressLogger
import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.RequestException
import kotlin.streams.toList
import kotlinx.coroutines.*

class ServiceAwait(private val aem: AemExtension) {

    private val options = aem.environmentOptions
    val progress = ProgressLogger.of(aem.project)

    fun checkForUnavailableServices() = progress.launch {
        val serviceStatuses = options.healthChecks.list.parallelStream().map { it.url to isServiceHealthy(it) }.toList()
        return@launch serviceStatuses.filter { !it.second }.map { it.first }
    }

    fun awaitConditionObservingProgress(message: String, maxAwaitTime: Long, condition: suspend () -> Boolean) = progress.launch {
        awaitCondition(message, maxAwaitTime, condition)
    }

    private fun ProgressLogger.isServiceHealthy(check: HealthCheck): Boolean {
        return awaitCondition("${check.url} - awaiting to start", check.maxAwaitTime) {
            isResponseValid(check)
        }
    }

    private fun ProgressLogger.awaitCondition(message: String, maxAwaitTime: Long, condition: suspend () -> Boolean): Boolean {
        var isValid = false
        runBlocking {
            withTimeoutOrNull(maxAwaitTime) {
                val startTime = System.currentTimeMillis()
                while (true) {
                    val timePassed = System.currentTimeMillis() - startTime
                    val remainingTime = (maxAwaitTime - timePassed) / Retry.SECOND_MILIS
                    progress("!$remainingTime $message")
                    delay(AWAIT_DELAY_DEFAULT)
                    isValid = condition()
                    if (isValid) {
                        return@withTimeoutOrNull
                    }
                }
            }
        }
        return isValid
    }

    private suspend fun isResponseValid(check: HealthCheck): Boolean {
        return coroutineScope {
            async {
                try {
                    val (status, body) = get(check.connectionTimeout, check.url)
                    status == check.status && body.contains(check.text)
                } catch (ex: RequestException) {
                    false
                }
            }.await()
        }
    }

    private fun http(connectionTimeout: Int): HttpClient {
        val http = HttpClient(aem.project)
        http.connectionTimeout = connectionTimeout
        return http
    }

    private fun get(connectionTimeout: Int, url: String): Pair<Int, String> {
        return http(connectionTimeout).get(url) { response ->
            val body = asString(response)
            val status = response.statusLine.statusCode
            status to body
        }
    }

    companion object {
        const val AWAIT_DELAY_DEFAULT = 500L
    }
}