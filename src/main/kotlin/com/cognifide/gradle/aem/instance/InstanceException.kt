package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemException

class InstanceException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
