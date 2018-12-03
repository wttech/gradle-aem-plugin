package com.cognifide.gradle.aem.bundle

import com.cognifide.gradle.aem.common.AemException

class BundleException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
