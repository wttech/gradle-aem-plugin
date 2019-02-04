package com.cognifide.gradle.aem.environment.docker

import java.net.URI

class DockerOptions {
    var stackName = "local-setup"
        set(value) {
            if (value.isBlank()) {
                throw DockerException("stackName cannot be blank!")
            }
            field = value
        }

    var composeFilePath = "local-environment/docker-compose.yml"

    val serviceHealthChecks = mutableListOf<HealthCheck>()

    fun healthCheck(url: String, block: HealthCheck.() -> Unit) {
        val check = HealthCheck(url)
        check.apply(block)
        serviceHealthChecks += check
    }
}

class HealthCheck(val url: String) {
    val uri = URI(url)
    var status = 200
    var text: String = ""
    var maxAwaitTime = 20000

    fun respondsWith(block: HealthCheck.() -> Unit) {
        this.apply(block)
    }
}