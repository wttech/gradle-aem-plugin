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
    fun downloadFrom(dirUrl: String, fileName: String, target: File)

    /**
     * Downloads file from specified URL.
     */
    fun download(fileUrl: String, target: File) {
        val dirUrl = fileUrl.substringBeforeLast("/")
        val fileName = fileUrl.substringAfterLast("/")

        downloadFrom(dirUrl, fileName, target)
    }

    /**
     * Uploads file to directory available at specified URL and set given name.
     */
    fun uploadTo(dirUrl: String, fileName: String, source: File)

    /**
     * Uploads file to directory available at specified URL.
     */
    fun uploadTo(dirUrl: String, source: File) {
        val fileName = source.name

        uploadTo(dirUrl, fileName, source)
    }

    /**
     * Uploads file to specified URL.
     */
    fun upload(fileUrl: String, source: File) {
        val dirUrl = fileUrl.substringBeforeLast("/")
        val fileName = fileUrl.substringAfterLast("/")

        uploadTo(dirUrl, fileName, source)
    }

    /**
     * Deletes file of given name in directory available at specified URL.
     */
    fun deleteFrom(dirUrl: String, fileName: String)

    /**
     * Deletes file available at specified URL.
     */
    fun delete(fileUrl: String) {
        val dirUrl = fileUrl.substringBeforeLast("/")
        val fileName = fileUrl.substringAfterLast("/")

        deleteFrom(dirUrl, fileName)
    }

    /**
     * Lists files in directory available at specified URL.
     */
    fun list(dirUrl: String): List<FileEntry>

    /**
     * Deletes all files in directory available at specified URL.
     */
    fun truncate(dirUrl: String)
}