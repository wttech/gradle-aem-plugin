package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.environment.EnvironmentException
import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

class Container(val name: String) {

    fun isRunning(): Boolean {
        val containerId = containerId() ?: return false

        try {
            return ProcBuilder("docker")
                    .withArgs("inspect", "-f", "{{.State.Running}}", containerId)
                    .run()
                    .outputString.trim().toBoolean()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to check container state '$name'!\n" +
                    "Error: '${e.stderr}'", e)
        }
    }

    @Suppress("SpreadOperator")
    fun exec(command: String, exitCode: Int = 0) {
        if (!isRunning()) {
            throw EnvironmentException("Cannot exec command '$command' since container '$name' is not running!")
        }

        val commands = command.split(" ").toTypedArray()

        ProcBuilder("docker")
                .withArgs("exec", containerId(), *commands)
                .withExpectedExitStatuses(exitCode)
                .run()
    }

    private fun containerId(): String? {
        try {
            val containerId = ProcBuilder("docker")
                    .withArgs("ps", "-l", "-q", "-f", "name=$name")
                    .run()
                    .outputString.trim()

            return if (containerId.isBlank()) {
                null
            } else {
                containerId
            }
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to load container id for name '$name'!\n" +
                    "Make sure your container is up and running before checking its id.\n" +
                    "Error: '${e.stderr}'", e)
        }
    }
}