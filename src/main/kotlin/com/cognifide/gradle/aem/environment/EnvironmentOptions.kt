package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.environment.docker.DockerOptions
import java.net.URI

class EnvironmentOptions {
    val docker = DockerOptions()
    val healthChecks = HealthChecks()

    fun healthChecks(configurer: HealthChecks.() -> Unit) {
        healthChecks.apply(configurer)
    }

    fun docker(configurer: DockerOptions.() -> Unit) {
        docker.apply(configurer)
    }
}

class HealthChecks {
    val list = mutableListOf<HealthCheck>()

    infix fun String.respondsWith(block: HealthCheck.() -> Unit) {
        val check = HealthCheck(this)
        check.apply(block)
        list += check
    }
}

class HealthCheck(val url: String) {
    val uri = URI(url)
    var status = 200
    var text: String = ""
    var maxAwaitTime = 20000
}