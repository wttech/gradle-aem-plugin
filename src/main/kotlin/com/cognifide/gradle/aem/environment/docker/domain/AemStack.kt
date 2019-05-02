package com.cognifide.gradle.aem.environment.docker.domain

import com.cognifide.gradle.aem.common.Behaviors
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.DockerStack

/**
 * Represents Docker stack named 'aem' and provides API for manipulating it.
 */
class AemStack(val environment: Environment) {

    private val aem = environment.aem

    private val stack = DockerStack("aem")

    var deployRetry = aem.retry { afterSecond(30) }

    var undeployRetry = aem.retry { afterSecond(30) }

    val running: Boolean
        get() = stack.running

    fun reset() {
        undeploy()
        deploy()
    }

    fun deploy() {
        aem.progressIndicator {
            message = "Starting AEM stack"
            stack.deploy(environment.dockerComposeFile.path)

            message = "Awaiting started AEM stack"
            Behaviors.waitUntil(deployRetry.delay) { timer ->
                val running = stack.running
                if (timer.ticks == deployRetry.times && !running) {
                    throw EnvironmentException("Failed to start Docker Stack!")
                }

                !running
            }
        }
    }

    fun undeploy() {
        aem.progressIndicator {
            message = "Stopping AEM stack"
            stack.rm()

            message = "Awaiting stopped AEM stack"
            Behaviors.waitUntil(undeployRetry.delay) { timer ->
                val running = stack.running
                if (timer.ticks == undeployRetry.times && running) {
                    throw EnvironmentException("Failed to stop Docker Stack! Try to stop manually using: 'docker stack rm ${stack.name}'")
                }

                running
            }
        }
    }
}