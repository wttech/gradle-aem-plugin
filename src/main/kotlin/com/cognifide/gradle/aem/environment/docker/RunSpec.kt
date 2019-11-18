package com.cognifide.gradle.aem.environment.docker

import org.gradle.process.internal.streams.SafeStreams
import java.io.File

class RunSpec : DockerDefaultSpec() {

    init {
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }

    var image: String = ""

    var volumes = mapOf<String, String>()

    var ports = mapOf<String, String>()

    fun port(hostPort: Int, containerPort: Int) = port(hostPort.toString(), containerPort.toString())

    fun port(hostPort: String, containerPort: String) {
        ports = ports + (hostPort to containerPort)
    }
    fun port(port: Int) = port(port, port)

    fun volume(localFile: File, containerPath: String) {
        volume(localFile.absolutePath, containerPath)
    }

    fun volume(localPath: String, containerPath: String) {
        volumes = volumes + (localPath to containerPath)
    }

    var operation: () -> String = {
        when {
            fullCommand.isBlank() -> "Running image '$image'"
            else -> "Running image '$image' and command '$fullCommand'"
        }
    }

    fun operation(operation: () -> String) {
        this.operation = operation
    }

    fun operation(text: String) = operation { text }
}
