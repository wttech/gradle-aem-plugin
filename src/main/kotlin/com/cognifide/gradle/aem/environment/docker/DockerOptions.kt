package com.cognifide.gradle.aem.environment.docker

class DockerOptions {
    var stackName = "local-setup"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("stackName cannot be blank!")
            }
            field = value
        }

    var composeFilePath = "local-environment/docker-compose.yml"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("composeFilePath cannot be blank!")
            }
            field = value
        }
}
