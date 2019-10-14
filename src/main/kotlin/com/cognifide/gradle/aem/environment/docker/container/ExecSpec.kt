package com.cognifide.gradle.aem.environment.docker.container

import com.cognifide.gradle.aem.environment.docker.base.DockerExecSpec

class ExecSpec : DockerExecSpec() {

    var operation: () -> String = { "Executing command '$command'" }

    fun operation(operation: () -> String) {
        this.operation = operation
    }
}
