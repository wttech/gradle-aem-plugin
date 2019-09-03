package com.cognifide.gradle.aem.environment.docker.domain

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.DockerContainer
import com.cognifide.gradle.aem.environment.docker.base.DockerException
import org.gradle.internal.os.OperatingSystem

class HttpdContainer(private val environment: Environment) {

    private val aem = environment.aem

    val container = DockerContainer(aem, aem.props.string("environment.httpdContainer.containerName") ?: "${aem.project.rootProject.name}_httpd")

    var awaitRetry = aem.retry { afterSecond(aem.props.long("environment.httpdContainer.awaitRetry") ?: 30) }

    var restartCommand = aem.props.string("environment.httpdContainer.restartCommand") ?: "/usr/local/apache2/bin/httpd -k restart"

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
                    throw EnvironmentException("Failed to restart HTTPD service! Check logs then configuration.", e)
                } else {
                    val processCause = e.processCause
                    if (processCause != null) {
                        aem.logger.error("Failed to restart HTTPD service, exit code: ${processCause.exitValue}")
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
                    val msg = mutableListOf("Failed to await HTTPD service!")
                    if (OperatingSystem.current().isWindows) {
                        msg.add("Ensure having shared drives configured and reset performed after changing Windows credentials.")
                    }
                    msg.add("Consider troubleshooting using command: 'docker stack ps ${environment.stack.stack.name} --no-trunc'.")
                    throw EnvironmentException(msg.joinToString("\n"))
                }

                !running
            }
        }
    }
}
