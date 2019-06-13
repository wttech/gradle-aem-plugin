package com.cognifide.gradle.aem.common.file.transfer.smb

import com.cognifide.gradle.aem.common.file.FileException

class SmbFileException : FileException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
