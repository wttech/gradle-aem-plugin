package com.cognifide.gradle.aem.common.utils

import org.apache.commons.codec.digest.DigestUtils
import java.io.File

object Checksum {

    fun md5(file: File) = file.inputStream().use { DigestUtils.md5Hex(it) }
}
