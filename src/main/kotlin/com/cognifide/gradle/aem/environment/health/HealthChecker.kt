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

        aem.progress(checks.size) {
            try {
                retry.launchSimply<Unit, EnvironmentException> {
                    reset()
                    all = aem.parallel.map(checks) { check ->
                        increment("Checking $check") {
                            check.perform()
                        }
                    }.toList()
                    passed = all.filter { it.passed }
                    failed = all - passed

                    if (failed.isNotEmpty()) {
                        throw EnvironmentException("There are failed environment health checks. Retrying...")
                    }
                }
            } catch (e: EnvironmentException) {
                val message = "Environment health check(s) failed (${passed.size}/${all.size} " +
                        "(${Formats.percent(passed.size, all.size)}):\n${failed.joinToString("\n")}"
                if (verbose) {
                    throw EnvironmentException(message)
                } else {
                    aem.logger.error(message)
                }
            }
        }
    }

    // Shorthand methods for defining health checks

    fun url(url: String, method: String = "GET", statusCode: Int = 200, text: String? = null) {
        define("URL $url") {
            Thread.sleep(1000) // TODO tmp

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