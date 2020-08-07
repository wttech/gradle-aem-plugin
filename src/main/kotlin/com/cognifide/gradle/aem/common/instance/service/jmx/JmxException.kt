package com.cognifide.gradle.aem.common.instance.service.jmx

import com.cognifide.gradle.aem.common.instance.InstanceException

open class JmxException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
