package com.cognifide.gradle.aem.environment.docker.stack

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.DockerException
import com.cognifide.gradle.aem.environment.docker.DockerProcess
import com.cognifide.gradle.aem.environment.docker.runtime.Toolbox

// TODO merge with base stack class
open class Stack(private val aem: AemExtension, val name: String) {

    var initTimeout = aem.props.long("environment.dockerStack.initTimeout") ?: 10000L

    var runningTimeout = aem.props.long("environment.dockerStack.runningTimeout") ?: 10000L

    fun init() {
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

    fun deploy(composeFilePath: String) {
        try {
            DockerProcess.exec { withArgs("stack", "deploy", "-c", composeFilePath, name) }
        } catch (e: DockerException) {
            throw StackException("Failed to deploy Docker stack '$name'!", e)
        }
    }

    fun rm() {
        try {
            DockerProcess.exec { withArgs("stack", "rm", name) }
        } catch (e: DockerException) {
            throw StackException("Failed to remove Docker stack '$name'!", e)
        }
    }

    val running: Boolean
        get() {
            val result = DockerProcess.execQuietly {
                withTimeoutMillis(runningTimeout)
                withArgs("network", "inspect", "${name}_docker-net")
            }
            return when {
                result.exitValue == 0 -> true
                result.errorString.contains("Error: No such network") -> false
                else -> throw StackException("Unable to determine Docker stack '$name' status. Error: '${result.errorString}'")
            }
        }
}
