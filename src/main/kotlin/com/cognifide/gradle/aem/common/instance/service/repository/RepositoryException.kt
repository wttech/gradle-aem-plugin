package com.cognifide.gradle.aem.common.instance.service.repository

import com.cognifide.gradle.aem.AemException

class RepositoryException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
