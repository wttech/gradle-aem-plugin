package com.cognifide.gradle.sling.common.instance.service.composum

import com.cognifide.gradle.sling.common.instance.InstanceException

class ComposumException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
