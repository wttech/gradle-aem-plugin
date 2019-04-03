package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.FileOperations
import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.stack.DeployConfigReader
import de.gesellix.docker.client.stack.DeployStackConfig
import de.gesellix.docker.client.stack.DeployStackOptions
import java.io.FileNotFoundException
import java.net.SocketException
import java.nio.file.Paths

class Stack(private val aem: AemExtension) {

    private val options = aem.environmentOptions.docker

    fun deploy(composeFilePath: String) {
        aem.logger.info("Deploying stack: ${options.stackName}")
        docker {
            initSwarmIfNotInitialized()
            stackDeploy(options.stackName, composeFilePath)
        }
    }

    fun rm() = docker { stackRm(options.stackName) }

    fun isStackDown(): Boolean {
        try {
            docker { inspectNetwork("${options.stackName}_docker-net") }
            return false
        } catch (e: DockerClientException) {
            val cause = e.cause
            if (cause is java.lang.IllegalStateException && cause.message == "docker network inspect failed") {
                return true
            }
            throw e
        }
    }

    private fun DockerClient.stackDeploy(stackName: String, composeFilePath: String) {
        try {
            stackDeploy(stackName, loadComposeConfig(stackName, composeFilePath), DeployStackOptions())
        } catch (dce: DockerClientException) {
            val cause = dce.cause
            if (cause is java.lang.IllegalStateException && cause.message == "docker service create failed") {
                aem.logger.warn(
                        "It is possible, that stack with the same name is still being switched off by docker." +
                                " Please wait few seconds before starting stack with the same name."
                )
            }
            throw DockerException("Failed to initialize service stack on docker!", dce)
        }
    }

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

    private fun docker(block: DockerClient.() -> Unit) =
            try {
                DockerClientImpl().block()
            } catch (e: FileNotFoundException) {
                if (e.message?.contains("docker_engine") == true) {
                    aem.logger.warn("It seems Docker is not installed on this machine and it is required to use Aem Environment plugin.\n" +
                            "Please install Docker and try again: https://docs.docker.com/docker-for-windows/install/")
                }
                throw DockerException("Failed to initialize Docker Swarm", e)
            } catch (e: SocketException) {
                if (e.message?.contains("Socket file not found: /private/var/run/docker.sock") == true) {
                    aem.logger.warn("It seems Docker is not installed on this machine and it is required to use Aem Environment plugin.\n" +
                            "Please install Docker and try again: https://docs.docker.com/install/")
                }
                throw DockerException("Failed to initialize Docker Swarm", e)
            }

    private fun DockerClient.initSwarmIfNotInitialized() {
        try {
            aem.logger.lifecycle("Swarm already initialized: $swarmManagerToken")
        } catch (dce: DockerClientException) {
            val cause = dce.cause
            if (cause is IllegalStateException && cause.message == "docker swarm inspect failed") {
                initSwarm()
                aem.logger.lifecycle("Swarm initialized")
            } else {
                throw DockerException("Failed to initialize Docker Swarm", dce)
            }
        }
    }
}
