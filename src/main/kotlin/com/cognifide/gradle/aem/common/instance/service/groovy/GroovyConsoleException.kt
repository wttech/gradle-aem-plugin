package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.common.instance.InstanceException

class GroovyConsoleException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
