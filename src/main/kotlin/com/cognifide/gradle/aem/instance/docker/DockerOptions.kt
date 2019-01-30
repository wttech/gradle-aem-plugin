package com.cognifide.gradle.aem.instance.docker

class DockerOptions {
    var stackName = "local-setup"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("stackName cannot be blank!")
            }
            field = value
        }

    var composeFilePath = "local-environment/docker-compose.yml"
}
