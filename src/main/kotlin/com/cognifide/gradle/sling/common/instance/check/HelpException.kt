package com.cognifide.gradle.sling.common.instance.check

import com.cognifide.gradle.sling.SlingException

open class HelpException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
