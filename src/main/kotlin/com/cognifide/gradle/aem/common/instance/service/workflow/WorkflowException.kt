package com.cognifide.gradle.aem.common.instance.service.workflow

import com.cognifide.gradle.aem.AemException

open class WorkflowException : AemException {
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String) : super(message)
}
