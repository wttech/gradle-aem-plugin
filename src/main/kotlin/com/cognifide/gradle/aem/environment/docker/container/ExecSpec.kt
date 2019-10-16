package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.environment.docker.base.DockerExecSpec
import org.gradle.process.internal.streams.SafeStreams

class ExecSpec : DockerExecSpec() {

    init {
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }

    var operation: () -> String = { "Executing command '$command'" }

    fun operation(operation: () -> String) {
        this.operation = operation
    }

    fun operation(text: String) = operation { text }
}
