package com.cognifide.gradle.aem.common.mvn

import com.cognifide.gradle.aem.AemException

class MvnException : AemException {
    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}