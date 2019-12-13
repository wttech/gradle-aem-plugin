package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.runtime.Toolbox

/**
 * Represents AEM project specific AEM Docker stack and provides API for manipulating it.
 */
class Stack(val environment: Environment) {

    private val aem = environment.aem

    val internalName = aem.prop.string("environment.docker.stack.name") ?: aem.project.rootProject.name

    var initTimeout = aem.prop.long("environment.docker.stack.initTimeout") ?: 10000L

    val initialized: Boolean by lazy {
        var error: Exception? = null

        aem.progressIndicator {
            message = "Initializing stack"

            try {
                initSwarm()
            } catch (e: DockerException) {
                error = e
            }
        }

        error?.let { e ->
            throw EnvironmentException("Stack cannot be initialized. Is Docker running / installed? Error '${e.message}'", e)
        }

        true
    }

    private fun initSwarm() {
        val result = DockerProcess.execQuietly {
            withTimeoutMillis(initTimeout)
            withArgs("swarm", "init")

            if (aem.environment.docker.runtime is Toolbox) {
                withArgs("--advertise-addr", aem.environment.docker.runtime.hostIp)
            }
        }
        if (result.exitValue != 0 && !result.errorString.contains("This node is already part of a swarm")) {
            throw StackException("Failed to initialize Docker Swarm. Is Docker running / installed? Error: '${result.errorString}'")
        }
    }

    var deployRetry = aem.retry { afterSecond(aem.prop.long("environment.docker.stack.deployRetry") ?: 30) }

    fun deploy() {
        aem.progressIndicator {
            message = "Starting stack '$internalName'"

            try {
                DockerProcess.exec { withArgs("stack", "deploy", "-c", environment.docker.composeFile.path, internalName) }
            } catch (e: DockerException) {
                throw StackException("Failed to deploy Docker stack '$internalName'!", e)
            }

            message = "Awaiting started stack '$internalName'"
            Behaviors.waitUntil(deployRetry.delay) { timer ->
                val running = networkAvailable
                if (timer.ticks == deployRetry.times && !running) {
                    throw EnvironmentException("Failed to start stack named '$internalName'!")
                }

                !running
            }
        }
    }

    var undeployRetry = aem.retry { afterSecond(aem.prop.long("environment.docker.stack.undeployRetry") ?: 30) }

    fun undeploy() {
        aem.progressIndicator {
            message = "Stopping stack '$internalName'"

            try {
                DockerProcess.exec { withArgs("stack", "rm", internalName) }
            } catch (e: DockerException) {
                throw StackException("Failed to remove Docker stack '$internalName'!", e)
            }

            message = "Awaiting stopped stack '$internalName'"
            Behaviors.waitUntil(undeployRetry.delay) { timer ->
                val running = networkAvailable
                if (timer.ticks == undeployRetry.times && running) {
                    throw EnvironmentException("Failed to stop stack named '$internalName'!" +
                            " Try to stop manually using Docker command: 'docker stack rm $internalName'")
                }

                running
            }
        }
    }

    var networkTimeout = aem.prop.long("environment.docker.stack.networkTimeout") ?: 10000L

    val networkAvailable: Boolean
        get() {
            val result = DockerProcess.execQuietly {
                withTimeoutMillis(networkTimeout)
                withArgs("network", "inspect", "${internalName}_docker-net")
            }
            return when {
                result.exitValue == 0 -> true
                result.errorString.contains("Error: No such network") -> false
                else -> throw StackException("Unable to determine Docker stack '$internalName' status. Error: '${result.errorString}'")
            }
        }

    val running: Boolean
        get() = initialized && networkAvailable

    fun reset() {
        undeploy()
        deploy()
    }
}
