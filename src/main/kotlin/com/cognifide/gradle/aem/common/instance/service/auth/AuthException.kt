package com.cognifide.gradle.aem.common.instance.service.auth

import com.cognifide.gradle.aem.common.instance.InstanceException

class AuthException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
