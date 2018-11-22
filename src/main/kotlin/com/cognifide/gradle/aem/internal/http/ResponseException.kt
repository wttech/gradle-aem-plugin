package com.cognifide.gradle.aem.internal.http

import com.cognifide.gradle.aem.api.AemException

class ResponseException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}