package com.cognifide.gradle.aem.common.instance.service.sling

import com.cognifide.gradle.aem.common.instance.InstanceException

open class SlingException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
