package com.cognifide.gradle.aem.environment.health

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException

class HealthChecker(val environment: Environment) {

    private val aem = environment.aem

    private val checks = mutableListOf<HealthCheck>()

    var retry = aem.retry { afterSecond(5) }

    fun define(name: String, check: () -> Unit) {
        checks += HealthCheck(name, check)
    }

    // Evaluation

    fun check(verbose: Boolean = true) {
        var all = listOf<HealthStatus>()
        var passed = listOf<HealthStatus>()
        var failed = listOf<HealthStatus>()
        val count by lazy { "${passed.size}/${all.size} (${Formats.percent(passed.size, all.size)})" }

        aem.progress(checks.size) {
            try {
                retry.launchSimply<Unit, EnvironmentException> { no ->
                    reset()

                    step = if (no > 1) {
                        "Checking (${failed.size} failed)"
                    } else {
                        "Checking"
                    }

                    all = aem.parallel.map(checks) { check ->
                        increment(check.name) {
                            check.perform()
                        }
                    }.toList()
                    passed = all.filter { it.passed }
                    failed = all - passed

                    if (failed.isNotEmpty()) {
                        throw EnvironmentException("There are failed environment health checks. Retrying...")
                    }
                }

                aem.logger.lifecycle("Environment health check(s) succeed: $count")
            } catch (e: EnvironmentException) {
                val message = "Environment health check(s) failed: $count:\n${failed.joinToString("\n")})"

                if (!verbose) {
                    aem.logger.error(message)
                } else {
                    throw EnvironmentException(message)
                }
            }
        }
    }

    // Shorthand methods for defining health checks

    fun url(url: String, method: String = "GET", statusCode: Int = 200, text: String? = null) {
        define("URL $url") {
            aem.http {
                call(method, url) { response ->
                    connectionRetries = false
                    connectionTimeout = 1000

                    checkStatus(response, statusCode)
                    if (text != null) {
                        checkText(response, text)
                    }
                }
            }
        }
    }
}