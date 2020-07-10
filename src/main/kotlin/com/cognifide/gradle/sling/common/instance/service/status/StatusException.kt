package com.cognifide.gradle.sling.common.instance.service.status

import com.cognifide.gradle.sling.common.instance.InstanceException

class StatusException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
