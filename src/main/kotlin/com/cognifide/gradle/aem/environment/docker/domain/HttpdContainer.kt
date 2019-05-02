package com.cognifide.gradle.aem.environment.docker.domain

import com.cognifide.gradle.aem.common.Behaviors
import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.DockerContainer
import org.buildobjects.process.ExternalProcessFailureException

class HttpdContainer(private val environment: Environment) {

    private val aem = environment.aem

    private val container = DockerContainer("aem_httpd")

    var awaitRetry = aem.retry { afterSecond(10) }

    val running: Boolean
        get() = container.running

    fun deploy() {
        await()
        restart()
    }

    fun restart() {
        aem.progressIndicator {
            message = "Restarting HTTPD service"

            try {
                container.exec("/usr/local/apache2/bin/httpd -k restart", 0)
            } catch (e: ExternalProcessFailureException) {
                throw EnvironmentException("Failed to reload HTTPD, exit code: ${e.exitValue}! Error:\n${Formats.logMessage(e.stderr)}")
            }
        }
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