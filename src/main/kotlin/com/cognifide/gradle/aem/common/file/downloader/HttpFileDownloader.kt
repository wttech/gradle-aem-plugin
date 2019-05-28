package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.IoTransferLogger
import com.cognifide.gradle.aem.common.http.HttpClient
import java.io.File
import java.io.IOException

class HttpFileDownloader(val aem: AemExtension, val client: HttpClient = HttpClient(aem)) {

    fun client(configurer: HttpClient.() -> Unit) {
        client.apply(configurer)
    }

    fun download(sourceUrl: String, targetFile: File) {
        try {
            client.get(sourceUrl) { response ->
                project.logger.info("Downloading: $sourceUrl -> ${targetFile.absolutePath}")
                IoTransferLogger(project).download(response.entity.contentLength, asStream(response), targetFile)
            }
        } catch (e: AemException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using HTTP(s). Cause: ${e.message}", e)
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using HTTP(s). Cause: ${e.message}", e)
        }
    }

    companion object {
        private val PROTOCOLS_HANDLED = arrayOf("http://", "https://")

        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isBlank() && (PROTOCOLS_HANDLED.any { sourceUrl.startsWith(it) })
        }
    }
}