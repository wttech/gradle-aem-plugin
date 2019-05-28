package com.cognifide.gradle.aem

import org.gradle.api.GradleException

open class AemException : GradleException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
