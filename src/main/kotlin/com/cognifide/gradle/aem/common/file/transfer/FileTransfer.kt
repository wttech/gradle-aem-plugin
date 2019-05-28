package com.cognifide.gradle.aem.common.file.transfer

import java.io.File

interface FileTransfer {

    fun download(url: String, name: String, target: File)

    fun upload(url: String, source: File)

    fun list(url: String): List<String>

    fun delete(url: String, name: String)

    fun truncate(url: String)

    fun handles(url: String): Boolean
}