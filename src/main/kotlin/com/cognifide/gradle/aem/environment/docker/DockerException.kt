package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.AemException

class DockerException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
