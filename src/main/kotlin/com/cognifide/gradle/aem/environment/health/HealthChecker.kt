package com.cognifide.gradle.aem.environment.health

import com.cognifide.gradle.aem.common.ProgressLogger
import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.RequestException
import com.cognifide.gradle.aem.environment.Environment
import kotlinx.coroutines.*

@UseExperimental(ObsoleteCoroutinesApi::class)
class HealthChecker(val environment: Environment) {

    private val aem = environment.aem

    val checks = mutableListOf<HealthCheck>()

    fun url(url: String, block: HealthCheck.() -> Unit) {
        checks += HealthCheck(url).apply(block)
    }

    private val progress = ProgressLogger.of(environment.aem.project)

    internal fun findUnavailable() = progress.launch {
        val serviceStatuses = aem.parallel.map(checks) {
            it.url to isHealthy(it)
        }
        return@launch serviceStatuses.filter { !it.second }.map { it.first }
    }

    private fun ProgressLogger.isHealthy(check: HealthCheck): Boolean {
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
            withContext(Dispatchers.Default) {
                try {
                    val (status, body) = get(check.connectionTimeout, check.url)
                    status == check.status && body.contains(check.text)
                } catch (ex: RequestException) {
                    false
                }
            }
        }
    }

    private fun http(timeout: Int) = HttpClient(aem).apply {
        connectionRetries = false
        connectionTimeout = timeout
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