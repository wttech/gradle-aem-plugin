package com.cognifide.gradle.aem.instance.docker

class DockerException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
