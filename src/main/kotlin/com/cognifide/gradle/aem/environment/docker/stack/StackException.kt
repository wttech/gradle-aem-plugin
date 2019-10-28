package com.cognifide.gradle.aem.environment.docker.stack

import com.cognifide.gradle.aem.environment.docker.DockerException

class StackException : DockerException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
