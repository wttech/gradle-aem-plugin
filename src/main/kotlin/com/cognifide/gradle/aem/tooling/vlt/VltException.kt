package com.cognifide.gradle.aem.tooling.vlt

import com.cognifide.gradle.aem.common.AemException

class VltException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
