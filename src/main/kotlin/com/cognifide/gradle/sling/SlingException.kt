package com.cognifide.gradle.sling

import com.cognifide.gradle.common.CommonException

open class SlingException : CommonException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
