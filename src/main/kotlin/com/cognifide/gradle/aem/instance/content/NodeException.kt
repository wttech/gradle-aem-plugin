package com.cognifide.gradle.aem.instance.content

import com.cognifide.gradle.aem.common.AemException

class NodeException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}