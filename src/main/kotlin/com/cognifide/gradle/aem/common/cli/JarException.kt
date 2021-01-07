package com.cognifide.gradle.aem.common.cli

import com.cognifide.gradle.aem.AemException

open class JarException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
