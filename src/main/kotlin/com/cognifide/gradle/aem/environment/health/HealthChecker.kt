package com.cognifide.gradle.aem.environment.health

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException

class HealthChecker(val environment: Environment) {

    private val aem = environment.aem

    private val checks = mutableListOf<HealthCheck>()

    fun define(name: String, check: () -> Unit) {
        checks += HealthCheck(name, check)
    }

    fun check() {
        aem.progress(checks.size) {
            val all = aem.parallel.map(checks) { check ->
                increment("Checking $check") {
                    check.perform()
                }
            }
            val passed = all.filter { it.passed }
            val failed = all - passed

            if (failed.isNotEmpty()) {
                aem.logger.error("Failed environment health checks:", failed.joinToString("\n"))
                throw EnvironmentException("Some environment health checks failed (${passed.size}/${all.size}" +
                        " (${Formats.percent(passed.size, all.size)})")
            }
        }
    }

    // Shorthand methods for defining health checks

    fun url(url: String, method: String = "GET", statusCode: Int = 200, text: String? = null) {
        define("URL $url") {
            Thread.sleep(1000) // TODO tmp

            aem.http {
                call(method, url) { response ->
                    checkStatus(response, statusCode)
                    if (text != null) {
                        checkText(response, text)
                    }
                }
            }
        }
    }
}