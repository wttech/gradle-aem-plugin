package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.AemException

open class HelpException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
