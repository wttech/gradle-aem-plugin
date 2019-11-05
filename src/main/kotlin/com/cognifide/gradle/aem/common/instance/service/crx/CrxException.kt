package com.cognifide.gradle.aem.common.instance.service.crx

import com.cognifide.gradle.aem.common.instance.InstanceException

class CrxException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
