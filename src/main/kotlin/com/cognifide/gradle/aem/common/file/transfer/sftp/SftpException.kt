package com.cognifide.gradle.aem.common.file.transfer.sftp

import com.cognifide.gradle.aem.common.file.FileException

class SftpException : FileException {

    constructor(message: String, cause: Throwable) : super(message, cause)

    constructor(message: String) : super(message)
}
