package com.cognifide.gradle.sling.common.pkg.vault

import com.cognifide.gradle.sling.SlingException

class VaultException : SlingException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
