package com.cognifide.gradle.aem.internal.file

import com.cognifide.gradle.aem.api.AemException

class FileException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
