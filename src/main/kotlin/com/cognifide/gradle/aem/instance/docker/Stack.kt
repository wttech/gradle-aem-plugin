package com.cognifide.gradle.aem.instance.docker

import com.cognifide.gradle.aem.common.AemExtension
import de.gesellix.docker.client.DockerClient
import de.gesellix.docker.client.DockerClientException
import de.gesellix.docker.client.DockerClientImpl
import de.gesellix.docker.client.stack.DeployConfigReader
import de.gesellix.docker.client.stack.DeployStackConfig
import de.gesellix.docker.client.stack.DeployStackOptions
import java.io.File
import java.nio.file.Paths

open class Stack(private val aem: AemExtension) {

    val namespace = "local-setup"
    val composeFilePath = "/Users/damian.mierzwinski/code/gradle-aem-multi/local-environment/docker-compose.yml"
    val workingDir = Paths.get(composeFilePath).parent.toString()

    fun initSwarm() = docker {
        try {
            initSwarm()
        } catch (e: DockerClientException) {
            aem.logger.debug("Swarm already initialized: ${e.message}")
        }
    }

    fun deploy() =
        docker {
            val composeStack = File(composeFilePath).inputStream()
            val deployConfig = DeployConfigReader(this).loadCompose(namespace, composeStack, workingDir, System.getenv()) as DeployStackConfig
            val options = DeployStackOptions()
            stackDeploy(namespace, deployConfig, options)
        }

    fun rm() = docker { stackRm(namespace) }

    private fun docker(block: DockerClient.() -> Unit) = DockerClientImpl().block()

    companion object {
        const val NAME = "dockerSetup"
    }
}
