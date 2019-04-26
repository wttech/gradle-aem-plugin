package com.cognifide.gradle.aem.common.file.transfer

import java.io.File

interface FileTransfer {
    fun download(name: String, target: File)

    fun upload(source: File)

    fun list(): List<String>

    fun delete(name: String)

    fun truncate()
}