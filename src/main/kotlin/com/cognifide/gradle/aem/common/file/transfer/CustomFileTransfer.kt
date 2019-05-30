package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import java.io.File

/**
 * Represents dynamically created file transfer via build script.
 *
 * Allows to implement file transfer supporting cloud storages like Amazon S3 and use them for e.g:
 *
 * - uploading AEM backups
 * - downloading AEM backups
 * - downloading CRX packages for instance satisfy task
 */
class CustomFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    override lateinit var name: String

    private var downloader: ((dirUrl: String, fileName: String, target: File) -> Unit)? = null

    private var uploader: ((dirUrl: String, fileName: String, target: File) -> Unit)? = null

    private var lister: ((dirUrl: String) -> List<FileEntry>)? = null

    private var deleter: ((dirUrl: String, fileName: String) -> Unit)? = null

    private var truncater: ((dirUrl: String) -> Unit)? = null

    override var protocols: List<String> = listOf()

    fun download(callback: (dirUrl: String, fileName: String, target: File) -> Unit) {
        this.downloader = callback
    }

    override fun download(dirUrl: String, fileName: String, target: File) {
        downloader?.invoke(dirUrl, fileName, target) ?: super.download(dirUrl, fileName, target)
    }

    fun upload(callback: (dirUrl: String, fileName: String, target: File) -> Unit) {
        this.uploader = callback
    }

    override fun upload(dirUrl: String, fileName: String, source: File) {
        uploader?.invoke(dirUrl, fileName, source) ?: super.upload(dirUrl, fileName, source)
    }

    fun list(callback: (dirUrl: String) -> List<FileEntry>) {
        this.lister = callback
    }

    override fun list(dirUrl: String): List<FileEntry> {
        return lister?.invoke(dirUrl) ?: super.list(dirUrl)
    }

    fun delete(callback: (dirUrl: String, fileName: String) -> Unit) {
        this.deleter = callback
    }

    override fun delete(dirUrl: String, fileName: String) {
        deleter?.invoke(dirUrl, fileName) ?: super.delete(dirUrl, fileName)
    }

    fun truncate(callback: (dirUrl: String) -> Unit) {
        this.truncater = callback
    }

    override fun truncate(dirUrl: String) {
        truncater?.invoke(dirUrl) ?: super.truncate(dirUrl)
    }
}