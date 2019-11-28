package com.cognifide.gradle.aem.common.file.transfer.generic

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

/**
 * Represents dynamically created file transfer via build script.
 *
 * Allows to implement file transfer supporting cloud storage like Amazon S3, Google Cloud Storage etc
 * and use them for example:
 *
 * - uploading AEM backups
 * - downloading AEM backups
 * - downloading CRX packages for instance satisfy task
 */
class CustomFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    override lateinit var name: String

    override lateinit var protocols: List<String>

    @get:JsonIgnore
    override var parallelable = true

    private var downloader: ((dirUrl: String, fileName: String, target: File) -> Unit)? = null

    private var uploader: ((dirUrl: String, fileName: String, target: File) -> Unit)? = null

    private var lister: ((dirUrl: String) -> List<FileEntry>)? = null

    private var deleter: ((dirUrl: String, fileName: String) -> Unit)? = null

    private var truncater: ((dirUrl: String) -> Unit)? = null

    private var exister: ((dirUrl: String, fileName: String) -> Boolean)? = null

    /**
     * Register callback responsible for downloading file.
     */
    fun download(callback: (dirUrl: String, fileName: String, target: File) -> Unit) {
        this.downloader = callback
    }

    /**
     * Register callback responsible for uploading file.
     */
    fun upload(callback: (dirUrl: String, fileName: String, target: File) -> Unit) {
        this.uploader = callback
    }

    /**
     * Register callback responsible for listing files.
     */
    fun list(callback: (dirUrl: String) -> List<FileEntry>) {
        this.lister = callback
    }

    /**
     * Register callback responsible for deleting file.
     */
    fun delete(callback: (dirUrl: String, fileName: String) -> Unit) {
        this.deleter = callback
    }

    /**
     * Register callback responsible for deleting files.
     */
    fun truncate(callback: (dirUrl: String) -> Unit) {
        this.truncater = callback
    }

    /**
     * Register callback responsible for checking file existence.
     */
    fun exists(callback: (dirUrl: String, fileName: String) -> Boolean) {
        this.exister = callback
    }

    // Below delegating lambdas to interface, nothing interesting :)

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        downloader?.invoke(dirUrl, fileName, target) ?: super.downloadFrom(dirUrl, fileName, target)
    }

    override fun uploadTo(dirUrl: String, fileName: String, source: File) {
        uploader?.invoke(dirUrl, fileName, source) ?: super.uploadTo(dirUrl, fileName, source)
    }

    override fun deleteFrom(dirUrl: String, fileName: String) {
        deleter?.invoke(dirUrl, fileName) ?: super.deleteFrom(dirUrl, fileName)
    }

    override fun list(dirUrl: String): List<FileEntry> {
        return lister?.invoke(dirUrl) ?: super.list(dirUrl)
    }

    override fun truncate(dirUrl: String) {
        truncater?.invoke(dirUrl) ?: super.truncate(dirUrl)
    }

    override fun exists(dirUrl: String, fileName: String): Boolean {
        return exister?.invoke(dirUrl, fileName) ?: super.exists(dirUrl)
    }
}
