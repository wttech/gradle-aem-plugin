package com.cognifide.gradle.aem.instance.docker

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.FileOperations
import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.stack.DeployConfigReader
import de.gesellix.docker.client.stack.DeployStackConfig
import de.gesellix.docker.client.stack.DeployStackOptions
import java.nio.file.Paths

class Stack(private val aem: AemExtension) {

    fun deploy() {
        aem.logger.lifecycle("Stack: ${aem.dockerOptions.stackName}")
        docker {
            initSwarmIfNotInitialized()
            stackDeploy(aem.dockerOptions.stackName, aem.dockerOptions.composeFilePath)
        }
    }

    fun rm() = docker { stackRm(aem.dockerOptions.stackName) }

    private fun DockerClient.stackDeploy(stackName: String, composeFilePath: String) =
        stackDeploy(stackName, loadComposeConfig(stackName, composeFilePath), DeployStackOptions())

    private fun DockerClient.loadComposeConfig(stackName: String, composeFilePath: String): DeployStackConfig {
        try {
            return FileOperations.streamFromPathOrClasspath(composeFilePath) { composeFileStream ->
                DeployConfigReader(this).loadCompose(
                    stackName,
                    composeFileStream,
                    Paths.get(composeFilePath).parent.toString(),
                    System.getenv()
                ) as DeployStackConfig
            }
        } catch (fe: FileException) {
            aem.logger.error("Cannot load docker compose file: $composeFilePath")
            throw DockerException("Cannot load docker compose file: $composeFilePath", fe)
        }
    }

    private fun docker(block: DockerClient.() -> Unit) = DockerClientImpl().block()

    private fun DockerClient.initSwarmIfNotInitialized() {
        try {
            aem.logger.lifecycle("Swarm already initialized: $swarmManagerToken")
        } catch (e: DockerClientException) {
            val cause = e.cause
            if (cause is IllegalStateException && cause.message == "docker swarm inspect failed") {
                initSwarm()
                aem.logger.lifecycle("Swarm initialized")
            } else {
                throw e
            }
        }
    }
}
