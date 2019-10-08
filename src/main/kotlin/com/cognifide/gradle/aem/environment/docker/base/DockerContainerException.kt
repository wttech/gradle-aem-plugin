package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.environment.docker.DockerException

class DockerContainerException : DockerException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
