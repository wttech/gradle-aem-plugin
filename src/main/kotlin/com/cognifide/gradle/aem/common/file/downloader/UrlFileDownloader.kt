package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.operation.FileDownloader
import com.cognifide.gradle.aem.common.utils.Patterns
import java.io.File
import java.io.IOException
import java.net.URL

class UrlFileDownloader(val aem: AemExtension) {

    fun download(sourceUrl: String, targetFile: File) {
        try {
            aem.logger.info("Downloading: $sourceUrl -> ${targetFile.absolutePath}")

            val connection = URL(sourceUrl).openConnection()
            connection.useCaches = false

            FileDownloader(aem).download(connection.contentLengthLong, connection.inputStream, targetFile)
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile'.", e)
        }
    }

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isBlank() && Patterns.wildcard(sourceUrl, "*://*")
        }
    }
}