package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.environment.docker.base.DockerSpec
import org.gradle.process.internal.streams.SafeStreams
import java.io.File

class RunSpec : DockerSpec() {

    init {
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }

    var image: String = ""

    var volumes = mutableMapOf<String, String>()

    fun volume(localFile: File, containerPath: String) = volume(localFile.absolutePath, containerPath)

    fun volume(localPath: String, containerPath: String) {
        volumes[localPath] = containerPath
    }

    var operation: () -> String = { "Running command '$command'" }

    fun operation(operation: () -> String) {
        this.operation = operation
    }

    fun operation(text: String) = operation { text }

}
