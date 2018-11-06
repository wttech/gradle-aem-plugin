package com.cognifide.gradle.aem.internal.http

import com.cognifide.gradle.aem.pkg.DeployException

class ResponseException : DeployException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}