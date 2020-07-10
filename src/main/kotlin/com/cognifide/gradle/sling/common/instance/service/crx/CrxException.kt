package com.cognifide.gradle.sling.common.instance.service.crx

import com.cognifide.gradle.sling.common.instance.InstanceException

class CrxException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
