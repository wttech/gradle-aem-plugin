package com.cognifide.gradle.sling.common.pkg

import com.cognifide.gradle.sling.SlingException

class PackageException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
