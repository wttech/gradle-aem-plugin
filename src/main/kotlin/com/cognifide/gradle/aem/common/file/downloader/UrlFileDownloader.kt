package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.utils.Patterns
import java.io.File
import java.io.IOException
import java.net.URL
import org.gradle.api.Project

class UrlFileDownloader(val project: Project) {

    fun download(sourceUrl: String, targetFile: File) {
        try {
            project.logger.info("Downloading: $sourceUrl -> ${targetFile.absolutePath}")

            val connection = URL(sourceUrl).openConnection()
            connection.useCaches = false

            val downloader = ProgressFileDownloader(project)
            downloader.size = connection.contentLengthLong
            downloader.download(connection.getInputStream(), targetFile)
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