package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.environment.docker.base.DockerContainer
import com.cognifide.gradle.aem.environment.docker.DockerException
import org.gradle.internal.os.OperatingSystem

class Container(private val environment: Environment, val name: String) {

    private val aem = environment.aem

    val base = DockerContainer(aem, name)

    var awaitRetry = aem.retry { afterSecond(aem.props.long("environment.container.awaitRetry") ?: 30) }

    var restartCommand = ""

    val running: Boolean
        get() = base.running

    fun deploy(): Boolean {
        await()

        return restart()
    }

    fun restart(verbose: Boolean = true): Boolean {
        var success = false

        aem.progressIndicator {
            message = "Restarting container '$name'"

            try {
                base.exec(restartCommand, 0)
                success = true
            } catch (e: DockerException) {
                success = false
                if (verbose) {
                    throw EnvironmentException("Failed to restart service '$name'! Check logs then configuration.", e)
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
                val running = base.running
                if (timer.ticks == awaitRetry.times && !running) {
                    val msg = mutableListOf("Failed to await HTTPD service!")
                    if (OperatingSystem.current().isWindows) {
                        msg.add("Ensure having shared drives configured and reset performed after changing Windows credentials.")
                    }
                    msg.add("Consider troubleshooting using command: 'docker stack ps ${environment.docker.stack.base.name} --no-trunc'.")
                    throw EnvironmentException(msg.joinToString("\n"))
                }

                !running
            }
        }
    }
}
