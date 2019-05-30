package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import java.io.File
import java.io.Serializable

abstract class AbstractFileTransfer(protected val aem: AemExtension) : FileTransfer, Serializable {

    override var enabled: Boolean = true

    override fun download(dirUrl: String, fileName: String, target: File) {
        throw FileException("File transfer '$name' does not support download operation.")
    }

    override fun upload(dirUrl: String, fileName: String, source: File) {
        throw FileException("File transfer '$name' does not support upload operation.")
    }

    override fun list(dirUrl: String): List<FileEntry> {
        throw FileException("File transfer '$name' does not support list operation.")
    }

    override fun delete(dirUrl: String, fileName: String) {
        throw FileException("File transfer '$name' does not support delete operation.")
    }

    override fun truncate(dirUrl: String) {
        throw FileException("File transfer '$name' does not support truncate operation.")
    }

    fun downloader() = FileDownloader(aem)

    fun uploader() = FileUploader(aem)
}