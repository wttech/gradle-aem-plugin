package com.cognifide.gradle.aem.common.java

import com.cognifide.gradle.aem.AemException

open class JavaException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
