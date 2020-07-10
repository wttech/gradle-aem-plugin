package com.cognifide.gradle.sling.common.cli

import com.cognifide.gradle.sling.SlingException

open class CliException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
