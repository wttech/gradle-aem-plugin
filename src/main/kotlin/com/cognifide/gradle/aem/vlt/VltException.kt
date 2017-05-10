package com.cognifide.gradle.aem.vlt

class VltException : Exception {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)

}
