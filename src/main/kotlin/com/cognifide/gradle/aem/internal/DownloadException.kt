package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.AemException

class DownloadException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
