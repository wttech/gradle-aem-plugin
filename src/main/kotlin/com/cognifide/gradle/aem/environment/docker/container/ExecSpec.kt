package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.environment.docker.DockerDefaultSpec
import org.gradle.process.internal.streams.SafeStreams

class ExecSpec : DockerDefaultSpec() {

    init {
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }

    fun workDir(path: String) {
        options = options + "--workdir $path"
    }

    fun user(id: String) {
        options = options + "--user $id"
    }

    fun env(vars: Map<String, String>) {
        vars.forEach { (varName, varValue) -> env(varName, varValue) }
    }

    fun env(varName: String, varValue: String) {
        options = options + "--env $varName=$varValue"
    }

    fun privileged() {
        options = options + "--privileged"
    }

    var operation: () -> String = { "Executing command '$command'" }

    fun operation(operation: () -> String) {
        this.operation = operation
    }

    fun operation(text: String) = operation { text }
}
