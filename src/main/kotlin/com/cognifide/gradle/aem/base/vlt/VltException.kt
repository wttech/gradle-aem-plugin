package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.base.api.AemException

class VltException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
