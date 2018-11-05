package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.api.AemException

open class DeployException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
