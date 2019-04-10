package com.cognifide.gradle.aem.environment.docker

import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

class Stack {

    fun deploy(composeFilePath: String) {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "deploy", "-c", composeFilePath, STACK_NAME_DEFAULT)
                    .withNoTimeout()
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to initialize stack '$STACK_NAME_DEFAULT' on docker! Error: '${e.stderr}'", e)
        }
    }

    fun rm() {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "rm", STACK_NAME_DEFAULT)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to remove stack '$STACK_NAME_DEFAULT' on docker! Error: '${e.stderr}'", e)
        }
    }

    fun isDown(): Boolean {
        val result = ProcBuilder("docker")
                .withArgs("network", "inspect", "${STACK_NAME_DEFAULT}_docker-net")
                .ignoreExitStatus()
                .run()
        if (result.exitValue == 0) {
            return false
        }
        if (result.errorString.contains("Error: No such network")) {
            return true
        }
        throw DockerException("Unable to determine stack '$STACK_NAME_DEFAULT' status. Error: '${result.errorString}'")
    }

    private fun containerId(containerName: String): String {
        try {
            return ProcBuilder("docker")
                    .withArgs("ps", "-l", "-q", "-f", "name=${STACK_NAME_DEFAULT}_$containerName")
                    .run()
                    .outputString.trim()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to load container id for name '${STACK_NAME_DEFAULT}_$containerName'!\n" +
                    "Make sure your container is up and running before checking its id.\n" +
                    "Error: '${e.stderr}'", e)
        }
    }

    @Suppress("SpreadOperator")
    fun exec(containerName: String, command: String, exitCode: Int = 0) {
        val containerId = containerId(containerName)
        val commands = command.split(" ").toTypedArray()
        ProcBuilder("docker")
                .withArgs("exec", containerId, *commands)
                .withExpectedExitStatuses(exitCode)
                .run()
    }

    companion object {
        const val STACK_NAME_DEFAULT = "aem-local-setup"
    }
}