package com.cognifide.gradle.aem

import com.cognifide.gradle.common.CommonException

open class AemException : CommonException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
