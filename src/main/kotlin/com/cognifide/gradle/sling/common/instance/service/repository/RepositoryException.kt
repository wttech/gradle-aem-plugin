package com.cognifide.gradle.sling.common.instance.service.repository

import com.cognifide.gradle.sling.common.instance.InstanceException

class RepositoryException : InstanceException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
