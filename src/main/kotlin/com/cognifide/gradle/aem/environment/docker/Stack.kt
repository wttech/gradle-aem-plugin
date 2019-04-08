package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemExtension
import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

class Stack(aem: AemExtension) {

    private val options = aem.environmentOptions.docker

    fun deploy(composeFilePath: String) {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "deploy", "-c", composeFilePath, options.stackName)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to initialize stack '${options.stackName}' on docker! Error: '${e.stderr}'", e)
        }
    }

    fun rm() {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "rm", options.stackName)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to remove stack '${options.stackName}' on docker! Error: '${e.stderr}'", e)
        }
    }

    fun isDown(): Boolean {
        val result = ProcBuilder("docker")
                .withArgs("network", "inspect", "${options.stackName}_docker-net")
                .ignoreExitStatus()
                .run()
        if (result.exitValue == 0) {
            return false
        }
        if (result.errorString.contains("Error: No such network")) {
            return true
        }
        throw DockerException("Unable to determine stack '${options.stackName}' status. Error: '${result.errorString}'")
    }

    private fun containerId(containerName: String): String {
        try {
            return ProcBuilder("docker")
                    .withArgs("ps", "-l", "-q", "-f", "name=${options.stackName}_$containerName")
                    .run()
                    .outputString.trim()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to load container id for name '${options.stackName}_$containerName'!\n" +
                    "Make sure your container is up and running before checking its id.\n" +
                    "Error: '${e.stderr}'", e)
        }
    }

    @Suppress("SpreadOperator")
    fun exec(containerName: String, command: String, exitCode: Int = 0) {
        val containerId = containerId(containerName)
        val commands = command.split(" ").toTypedArray()
        try {
            ProcBuilder("docker")
                    .withArgs("exec", containerId, *commands)
                    .withExpectedExitStatuses(exitCode)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to exec command '${e.command}' on a container '$containerId'.\n" +
                    "Error: '${e.stderr}'", e)
        }
    }
}