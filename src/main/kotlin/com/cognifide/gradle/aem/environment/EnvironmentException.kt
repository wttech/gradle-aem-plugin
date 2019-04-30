package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemException

class EnvironmentException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}