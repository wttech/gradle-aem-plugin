package com.cognifide.gradle.aem.environment.docker

import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

class Stack(val name: String = STACK_NAME_DEFAULT) {

    fun deploy(composeFilePath: String) {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "deploy", "-c", composeFilePath, name)
                    .withNoTimeout()
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to initialize stack '$name' on docker! Error: '${e.stderr}'", e)
        }
    }

    fun rm() {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "rm", name)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to remove stack '$name' on docker! Error: '${e.stderr}'", e)
        }
    }

    fun isDown(): Boolean {
        val result = ProcBuilder("docker")
                .withArgs("network", "inspect", "${name}_docker-net")
                .ignoreExitStatus()
                .run()
        if (result.exitValue == 0) {
            return false
        }
        if (result.errorString.contains("Error: No such network")) {
            return true
        }
        throw DockerException("Unable to determine stack '$name' status. Error: '${result.errorString}'")
    }

    companion object {
        private const val STACK_NAME_DEFAULT = "aem-local-setup"
    }
}