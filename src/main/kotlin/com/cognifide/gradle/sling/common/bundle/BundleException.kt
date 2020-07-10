package com.cognifide.gradle.sling.common.bundle

import com.cognifide.gradle.sling.SlingException

class BundleException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
