package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.ProgressLogger
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.common.http.RequestException
import java.nio.charset.Charset
import kotlin.streams.toList
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.commons.io.IOUtils

class ServiceAwait(aem: AemExtension) {

    private val http = HttpClient(aem.project)
    private val options = aem.dockerOptions
    val progress = ProgressLogger.of(aem.project)

    fun await() =
        progress.launch {
            val serviceStatuses = options.serviceHealthChecks.parallelStream().map { it.url to healthy(it) }.toList()
            if (!serviceStatuses.all { it.second }) {
                val unavailableUrls = serviceStatuses.filter { !it.second }.map { it.first }.joinToString("\n")
                throw DockerException("Failed to initialized all services! Following URLs are still unavailable " +
                    "or returned different response than expected:\n$unavailableUrls")
            }
        }

    private fun ProgressLogger.healthy(check: HealthCheck): Boolean {
        var isValid = false
        runBlocking {
            val noOfChecks = noOfChecks(check)
            repeat(noOfChecks) { iteration ->
                progress("$iteration/$noOfChecks ${check.url} - awaiting to start")
                delay(DELAY_BETWEEN_CHECKS.toLong())
                try {
                    val (status, body) = get(check.url)
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

    private fun get(url: String): Pair<Int, String> {
        return http.get(url) { response ->
            val body = response.entity.content.use { IOUtils.toString(it, Charset.forName("UTF-8")) }
            val status = response.statusLine.statusCode
            status to body
        }
    }

    private fun noOfChecks(check: HealthCheck) = Math.max(check.maxAwaitTime / DELAY_BETWEEN_CHECKS, 1)

    companion object {
        private const val DELAY_BETWEEN_CHECKS = 1000
    }
}