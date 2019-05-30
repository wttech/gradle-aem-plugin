package com.cognifide.gradle.aem.common.file.transfer

import java.io.File

interface FileTransfer {

    /**
     * Unique identifier.
     */
    val name: String

    /**
     * When enabled, transfer will be considered when finding transfer handling particular URL.
     */
    val enabled: Boolean

    /**
     * Checks if supports particular URL
     */
    fun handles(fileUrl: String): Boolean

    /**
     * Downloads file of given name from directory available at specified URL.
     */
    fun download(dirUrl: String, fileName: String, target: File)

    /**
     * Downloads file available at specified URL.
     */
    fun download(fileUrl: String, target: File) {
        val dirUrl = fileUrl.substringBeforeLast("/")
        val fileName = fileUrl.substringAfterLast("/")

        download(dirUrl, fileName, target)
    }

    /**
     * Uploads file to directory available at specified URL and set given name.
     */
    fun upload(dirUrl: String, fileName: String, source: File)

    /**
     * Uploads file to directory available at specified URL.
     */
    fun upload(dirUrl: String, source: File) {
        val fileName = source.name

        upload(dirUrl, fileName, source)
    }

    /**
     * Lists files in directory available at specified URL.
     */
    fun list(dirUrl: String): List<FileEntry>

    /**
     * Deletes file of given name in directory available at specified URL.
     */
    fun delete(dirUrl: String, fileName: String)

    /**
     * Deletes file available at specified URL.
     */
    fun delete(fileUrl: String) {
        val dirUrl = fileUrl.substringBeforeLast("/")
        val fileName = fileUrl.substringAfterLast("/")

        delete(dirUrl, fileName)
    }

    /**
     * Deletes all files in directory available at specified URL.
     */
    fun truncate(dirUrl: String)
}