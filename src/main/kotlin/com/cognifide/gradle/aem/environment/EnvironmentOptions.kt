package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.environment.docker.DockerOptions
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import java.net.URI
import kotlin.math.max

class EnvironmentOptions {
    val healthChecks = HealthChecks()
    val hosts = HostsOptions()
    val docker = DockerOptions()

    fun healthChecks(configurer: HealthChecks.() -> Unit) {
        healthChecks.apply(configurer)
    }

    fun hosts(config: Map<String, String>) {
        hosts.configure(config)
    }

    fun docker(configurer: DockerOptions.() -> Unit) {
        docker.apply(configurer)
    }
}

class HealthChecks {
    val list = mutableListOf<HealthCheck>()

    infix fun String.respondsWith(block: HealthCheck.() -> Unit) {
        list += HealthCheck(this).apply(block)
    }
}

class HealthCheck(val url: String) {
    val uri = URI(url)
    var status = 200
    var text: String = ""
    var maxAwaitTime = 60000
    var connectionTimeout = 3000
    var delay = Retry.SECOND_MILIS

    fun numberOfChecks() = max(maxAwaitTime / connectionTimeout, 1)
    fun timeLeft(iteration: Int) = (maxAwaitTime - (connectionTimeout + delay) * iteration) / Retry.SECOND_MILIS
}