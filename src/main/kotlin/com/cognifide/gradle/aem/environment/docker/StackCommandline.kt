package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemExtension
import org.buildobjects.process.ExternalProcessFailureException
import org.buildobjects.process.ProcBuilder

class StackCommandline(aem: AemExtension) : Stack {

    private val options = aem.environmentOptions.docker

    override fun deploy(composeFilePath: String) {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "deploy", "-c", composeFilePath, options.stackName)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to initialize stack '${options.stackName}' on docker! Error: '${e.stderr}'", e)
        }
    }

    override fun rm() {
        try {
            ProcBuilder("docker")
                    .withArgs("stack", "rm", options.stackName)
                    .run()
        } catch (e: ExternalProcessFailureException) {
            throw DockerException("Failed to remove stack '${options.stackName}' on docker! Error: '${e.stderr}'", e)
        }
    }

    override fun isDown(): Boolean {
        val result = ProcBuilder("docker")
                .withArgs("service", "inspect", options.stackName)
                .ignoreExitStatus()
                .run()
        if (result.exitValue == 0) {
            return false
        }
        if (result.errorString.contains("Status: Error: no such service")) {
            return true
        }
        throw DockerException("Unable to determine stack '${options.stackName}' status. Error: '${result.errorString}'")
    }
}