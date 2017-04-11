package com.cognifide.gradle.aem.deploy

class DeployException : Exception {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
