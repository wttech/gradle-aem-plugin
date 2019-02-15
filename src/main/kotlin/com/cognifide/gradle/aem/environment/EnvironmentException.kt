package com.cognifide.gradle.aem.environment

class EnvironmentException : RuntimeException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}