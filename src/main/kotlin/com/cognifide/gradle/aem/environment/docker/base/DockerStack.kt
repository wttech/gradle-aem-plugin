package com.cognifide.gradle.aem.environment.docker.base

open class DockerStack(val name: String) {

    fun init() {
        val result = Docker.execQuietly { withArgs("swarm", "init") }
        if (result.exitValue != 0 && !result.errorString.contains("This node is already part of a swarm")) {
            throw DockerStackException("Failed to initialize Docker Swarm. Is Docker installed? Error: '${result.errorString}'")
        }
    }

    fun deploy(composeFilePath: String) {
        try {
            Docker.exec { withArgs("stack", "deploy", "-c", composeFilePath, name) }
        } catch (e: DockerException) {
            throw DockerStackException("Failed to initialize stack '$name' on Docker!", e)
        }
    }

    fun rm() {
        try {
            Docker.exec { withArgs("stack", "rm", name) }
        } catch (e: DockerException) {
            throw DockerStackException("Failed to remove stack '$name' on Docker!", e)
        }
    }

    val running: Boolean
        get() {
            val result = Docker.execQuietly { withArgs("network", "inspect", "${name}_docker-net") }
            return when {
                result.exitValue == 0 -> true
                result.errorString.contains("Error: No such network") -> false
                else -> throw DockerStackException("Unable to determine stack '$name' status. Error: '${result.errorString}'")
            }
        }
}