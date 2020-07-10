package com.cognifide.gradle.sling.common.instance.service.osgi

import com.cognifide.gradle.sling.common.instance.InstanceException

open class OsgiException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
