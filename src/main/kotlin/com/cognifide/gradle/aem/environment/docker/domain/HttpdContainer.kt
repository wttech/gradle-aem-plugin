package com.cognifide.gradle.aem.environment.docker.domain

import com.cognifide.gradle.aem.common.Behaviors
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.DockerContainer
import com.cognifide.gradle.aem.environment.docker.base.DockerException

class HttpdContainer(environment: Environment) {

    private val aem = environment.aem

    val container = DockerContainer("aem_httpd")

    var awaitRetry = aem.retry { afterSecond(10) }

    var restartCommand = "/usr/local/apache2/bin/httpd -k restart"

    val running: Boolean
        get() = container.running

    fun deploy(): Boolean {
        await()

        return restart()
    }

    fun restart(verbose: Boolean = true): Boolean {
        var success = false

        aem.progressIndicator {
            message = "Restarting HTTPD service"

            try {
                container.exec(restartCommand, 0)
                success = true
            } catch (e: DockerException) {
                success = false
                if (verbose) {
                    throw EnvironmentException("Failed to reload HTTPD service!", e)
                } else {
                    val processException = e.processException
                    if (processException != null) {
                        aem.logger.error("----------------------------------------------------------------------------")
                        aem.logger.error("Failed to reload HTTPD service, exit code: ${processException.exitValue}")
                        if (!processException.stderr.isNullOrBlank()) {
                            aem.logger.error(processException.stderr)
                        }
                        aem.logger.error("----------------------------------------------------------------------------")
                    }
                }
            }
        }

        return success
    }

    fun await() {
        aem.progressIndicator {
            message = "Awaiting HTTPD service"
            Behaviors.waitUntil(awaitRetry.delay) { timer ->
                val running = container.running
                if (timer.ticks == awaitRetry.times && !running) {
                    throw EnvironmentException("Failed to restart HTTPD service")
                }

                !running
            }
        }
    }
}