package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemException

class VaultException : AemException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
