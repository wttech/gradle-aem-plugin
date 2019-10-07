package com.cognifide.gradle.aem.common.file.transfer.dependency

import com.cognifide.gradle.aem.common.file.FileException

class DependencyFileException : FileException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
