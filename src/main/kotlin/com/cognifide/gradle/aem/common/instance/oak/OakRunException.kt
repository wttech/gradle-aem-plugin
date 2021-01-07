package com.cognifide.gradle.aem.common.instance.oak

import com.cognifide.gradle.aem.AemException

open class OakRunException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
