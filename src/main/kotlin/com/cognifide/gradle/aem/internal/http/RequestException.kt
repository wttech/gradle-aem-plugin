package com.cognifide.gradle.aem.internal.http

import com.cognifide.gradle.aem.api.AemException

class RequestException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}