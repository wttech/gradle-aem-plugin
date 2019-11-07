package com.cognifide.gradle.aem.environment.docker

class StackException : DockerException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
