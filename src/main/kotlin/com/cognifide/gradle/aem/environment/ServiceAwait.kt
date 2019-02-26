package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.ProgressLogger
import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.RequestException
import kotlin.streams.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class ServiceAwait(private val aem: AemExtension) {

    private val options = aem.environmentOptions
    val progress = ProgressLogger.of(aem.project)

    fun await() =
            progress.launch {
                val serviceStatuses = options.healthChecks.list.parallelStream().map { it.url to healthy(it) }.toList()
                if (!serviceStatuses.all { it.second }) {
                    val unavailableUrls = serviceStatuses.filter { !it.second }.map { it.first }.joinToString("\n")
                    throw EnvironmentException("Failed to initialized all services! Following URLs are still unavailable " +
                            "or returned different response than expected:\n$unavailableUrls")
                }
            }

    fun await(message: String, retry: Retry, condition: () -> Boolean) {
        progress.launch {
            runBlocking {
                val noOfChecks = retry.times.toInt()
                repeat(noOfChecks) { iteration ->
                    progress("!${noOfChecks - iteration} $message")
                    delay(retry.delay(iteration.toLong()))
                    if (condition()) {
                        return@runBlocking
                    }
                }
                throw EnvironmentException("Failed to stop docker stack after ${retry.times} seconds." +
                        "\nPlease try to stop it manually by running: `docker stack rm ${options.docker.stackName}`")
            }
        }
    }

    private fun ProgressLogger.healthy(check: HealthCheck): Boolean {
        var isValid = false
        runBlocking {
            val noOfChecks = check.numberOfChecks()
            repeat(noOfChecks) { iteration ->
                progress("!${check.timeLeft(iteration)} ${check.url} - awaiting to start")
                delay(check.delay)
                try {
                    val (status, body) = get(check.connectionTimeout, check.url)
                    if (status == check.status && body.contains(check.text)) {
                        isValid = true
                        return@runBlocking
                    }
                } catch (ex: RequestException) {
                    // wait for service to start
                }
            }
        }
        return isValid
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
}