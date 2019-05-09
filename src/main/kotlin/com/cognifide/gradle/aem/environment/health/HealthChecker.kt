package com.cognifide.gradle.aem.environment.health

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.http.HttpClient
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException

class HealthChecker(val environment: Environment) {

    private val aem = environment.aem

    private val checks = mutableListOf<HealthCheck>()

    private var urlOptions: HttpClient.() -> Unit = {
        connectionRetries = false
        connectionTimeout = 1000
    }

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
                retry.withSleep<Unit, EnvironmentException> { no ->
                    reset()

                    step = if (no > 1) {
                        "Health rechecking (${failed.size} failed)"
                    } else {
                        "Health checking"
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
                val message = "Environment health check(s) failed: $count:\n${all.joinToString("\n")}"

                if (!verbose) {
                    aem.logger.error(message)
                } else {
                    throw EnvironmentException(message)
                }
            }
        }
    }

    // Shorthand methods for defining health checks

    fun url(checkName: String, url: String, method: String = "GET", statusCode: Int = 200, text: String? = null) {
        define(checkName) {
            aem.http {
                request(method, url) { response ->
                    apply(urlOptions)

                    checkStatus(response, statusCode)
                    if (text != null) {
                        checkText(response, text)
                    }
                }
            }
        }
    }

    fun urlOptions(options: HttpClient.() -> Unit) {
        this.urlOptions = options
    }
}