package com.cognifide.gradle.sling.common.instance

import com.cognifide.gradle.sling.SlingException

open class InstanceException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
