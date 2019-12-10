package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.AemExtension
import org.gradle.process.internal.streams.SafeStreams

class DaemonSpec(aem: AemExtension) : RunSpec(aem) {

    var initTime = 3_000L

    var stopPrevious = true

    var unique = false

    var id: String? = null

    val outputFile get() = aem.project.file("build/aem/environment/docker/$id.log")

    init {
        input = SafeStreams.emptyInput()
        output = null
        errors = null

        cleanup = true
    }
}
