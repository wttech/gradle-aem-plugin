package com.cognifide.gradle.aem.common.file.downloader

import java.io.File

interface FileTransfer {
    fun download(url: String, target: File)

    fun upload(source: File, url: String)

    fun delete(url: String)
}