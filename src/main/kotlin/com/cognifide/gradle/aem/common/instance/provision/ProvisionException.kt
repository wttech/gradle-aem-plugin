package com.cognifide.gradle.aem.common.instance.provision

import com.cognifide.gradle.aem.common.instance.InstanceException

open class ProvisionException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
