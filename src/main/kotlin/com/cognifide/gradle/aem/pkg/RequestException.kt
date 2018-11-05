package com.cognifide.gradle.aem.pkg

class RequestException : DeployException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}