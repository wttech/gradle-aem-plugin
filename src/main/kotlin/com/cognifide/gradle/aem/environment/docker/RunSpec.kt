package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.AemExtension
import org.gradle.process.internal.streams.SafeStreams
import java.io.File
import java.util.*

open class RunSpec(aem: AemExtension) : DockerDefaultSpec(aem) {

    var name: String? = null

    var image: String = ""

    var volumes = mapOf<String, String>()

    var ports = mapOf<String, String>()

    fun port(hostPort: Int, containerPort: Int) = port(hostPort.toString(), containerPort.toString())

    fun port(hostPort: String, containerPort: String) {
        ports = ports + (hostPort to containerPort)
    }

    fun port(port: Int) = port(port, port)

    fun volume(localFile: File, containerPath: String) = volume(localFile.absolutePath, containerPath)

    fun volume(localPath: String, containerPath: String) {
        volumes = volumes + (localPath to containerPath)
    }

    var cleanup: Boolean = false

    var detached: Boolean = false

    private var operartonProvider: () -> String = {
        when {
            fullCommand.isBlank() -> "Running image '$image'"
            else -> "Running image '$image' and command '$fullCommand'"
        }
    }

    val operation: String get() = operartonProvider()

    fun operation(textProvider: () -> String) {
        this.operartonProvider = textProvider
    }

    fun operation(text: String) = operation { text }

    var indicator = true

    init {
        input = SafeStreams.emptyInput()
        output = SafeStreams.systemOut()
        errors = SafeStreams.systemErr()
    }
}
