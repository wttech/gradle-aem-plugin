package com.cognifide.gradle.sling.common.instance.provision

import com.cognifide.gradle.sling.common.instance.InstanceException

open class ProvisionException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
