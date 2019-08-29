package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.common.instance.InstanceException

class WorkflowException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
