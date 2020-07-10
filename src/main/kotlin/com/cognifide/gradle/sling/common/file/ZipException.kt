package com.cognifide.gradle.sling.common.file

import com.cognifide.gradle.sling.SlingException

class ZipException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
