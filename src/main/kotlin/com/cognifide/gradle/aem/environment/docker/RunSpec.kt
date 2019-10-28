package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.environment.docker.base.DockerSpec
import org.gradle.process.internal.streams.SafeStreams

class RunSpec : DockerSpec() {

    init {
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }

    lateinit var image: String

    var volumes = mutableMapOf<String, String>()

    fun volume(localPath: String, containerPath: String) {
        volumes[localPath] = containerPath
    }

    var operation: () -> String = { "Running command '$command'" }

    fun operation(operation: () -> String) {
        this.operation = operation
    }

    fun operation(text: String) = operation { text }

}
