package com.cognifide.gradle.sling.common.instance

open class LocalInstanceException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
