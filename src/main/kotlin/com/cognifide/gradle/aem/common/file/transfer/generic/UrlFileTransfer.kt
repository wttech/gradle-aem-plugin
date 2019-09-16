package com.cognifide.gradle.aem.common.file.transfer.generic

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import java.io.File
import java.io.IOException
import java.net.URL

class UrlFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("*://*")

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        val fileUrl = "$dirUrl/$fileName"

        try {
            aem.logger.info("Downloading: $fileUrl -> ${target.absolutePath}")

            URL(fileUrl).openConnection().apply {
                useCaches = false
                inputStream.use { downloader().download(contentLengthLong, it, target) }
            }
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$fileUrl' to file '$target'. Cause: '${e.message}'", e)
        }
    }

    companion object {
        const val NAME = "url"
    }
}
