package com.cognifide.gradle.aem.common.file.transfer.http

import com.cognifide.gradle.aem.common.file.FileException

class HttpFileException : FileException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
