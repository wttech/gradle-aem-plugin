package com.cognifide.gradle.aem.environment.docker

class DockerOptions {
    var stackName = "local-setup"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("Stack name is required to run aemEnvUp (environment -> docker -> stackName)!")
            }
            field = value
        }

    var composeFilePath = "local-environment/docker-compose.yml"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("Compose file path is required to run aemEnvUp (environment -> docker -> composeFilePath)!")
            }
            field = value
        }

    var downDelay = 30L
}
