package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemException

class ImsException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
