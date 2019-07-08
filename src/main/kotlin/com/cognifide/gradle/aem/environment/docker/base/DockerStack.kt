package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.base.runtime.Toolbox

open class DockerStack(private val aem: AemExtension, val name: String) {

    var initTimeout = aem.props.long("environment.dockerStack.initTimeout") ?: 10000L

    var runningTimeout = aem.props.long("environment.dockerStack.runningTimeout") ?: 10000L

    fun init() {
        val result = Docker.execQuietly {
            withTimeoutMillis(initTimeout)
            withArgs("swarm", "init")

            if (aem.environment.dockerRuntime is Toolbox) {
                withArgs("--advertise-addr", aem.environment.dockerRuntime.hostIp)
            }
        }
        if (result.exitValue != 0 && !result.errorString.contains("This node is already part of a swarm")) {
            throw DockerStackException("Failed to initialize Docker Swarm. Is Docker running / installed? Error: '${result.errorString}'")
        }
    }

    fun deploy(composeFilePath: String) {
        try {
            Docker.exec { withArgs("stack", "deploy", "-c", composeFilePath, name) }
        } catch (e: DockerException) {
            throw DockerStackException("Failed to deploy Docker stack '$name'!", e)
        }
    }

    fun rm() {
        try {
            Docker.exec { withArgs("stack", "rm", name) }
        } catch (e: DockerException) {
            throw DockerStackException("Failed to remove Docker stack '$name'!", e)
        }
    }

    val running: Boolean
        get() {
            val result = Docker.execQuietly {
                withTimeoutMillis(runningTimeout)
                withArgs("network", "inspect", "${name}_docker-net")
            }
            return when {
                result.exitValue == 0 -> true
                result.errorString.contains("Error: No such network") -> false
                else -> throw DockerStackException("Unable to determine Docker stack '$name' status. Error: '${result.errorString}'")
            }
        }
}