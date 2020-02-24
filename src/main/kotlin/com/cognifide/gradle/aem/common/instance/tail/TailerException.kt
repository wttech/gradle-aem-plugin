package com.cognifide.gradle.aem.common.instance.tail

import com.cognifide.gradle.aem.common.instance.InstanceException

class TailerException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
