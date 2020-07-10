package com.cognifide.gradle.sling.common.instance.tail

import com.cognifide.gradle.sling.common.instance.InstanceException

class TailerException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
