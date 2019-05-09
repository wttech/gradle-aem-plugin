package com.cognifide.gradle.aem.environment.docker.base

class DockerStackException : DockerException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
