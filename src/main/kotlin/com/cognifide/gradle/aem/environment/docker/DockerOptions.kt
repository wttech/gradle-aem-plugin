package com.cognifide.gradle.aem.environment.docker

class DockerOptions {
    var stackName = "local-setup"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("Stack name is required to run aemEnvUp (environment -> docker -> stackName)!")
            }
            field = value
        }
}
