package com.cognifide.gradle.aem.common.http

import com.cognifide.gradle.aem.common.AemException

class RequestException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}