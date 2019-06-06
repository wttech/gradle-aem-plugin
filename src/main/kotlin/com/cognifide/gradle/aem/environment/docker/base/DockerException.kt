package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.AemException
import org.buildobjects.process.ExternalProcessFailureException

open class DockerException : AemException {

    var processCause: ExternalProcessFailureException? = null

    constructor(message: String, cause: ExternalProcessFailureException) : super(message, cause) {
        processCause = cause
    }

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
