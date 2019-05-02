package com.cognifide.gradle.aem.environment.docker.base

import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

open class DockerStack(val name: String) {

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

    val running: Boolean
        get() {
            val result = ProcBuilder("docker")
                    .withArgs("network", "inspect", "${name}_docker-net")
                    .ignoreExitStatus()
                    .run()

            if (result.exitValue == 0) {
                return true
            }
            if (result.errorString.contains("Error: No such network")) {
                return false
            }

            throw DockerException("Unable to determine stack '$name' status. Error: '${result.errorString}'")
        }
}