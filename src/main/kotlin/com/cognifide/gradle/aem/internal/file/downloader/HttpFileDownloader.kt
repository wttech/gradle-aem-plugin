package com.cognifide.gradle.aem.internal.file.downloader

import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.internal.file.FileException
import com.cognifide.gradle.aem.internal.http.HttpClient
import java.io.File
import java.io.IOException
import org.gradle.api.Project

class HttpFileDownloader(
    val project: Project,
    val client: HttpClient = HttpClient(project)
) {

    fun client(configurer: HttpClient.() -> Unit) {
        client.apply(configurer)
    }

    fun download(sourceUrl: String, targetFile: File) {
        try {
            client.get(sourceUrl) { response ->
                val downloader = ProgressFileDownloader(project)
                downloader.headerSourceTarget(sourceUrl, targetFile)
                downloader.size = response.entity.contentLength

                downloader.download(asStream(response), targetFile)
            }
        } catch (e: AemException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using HTTP(s). Check connection.", e)
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using HTTP(s). Check connection.", e)
        }
    }

    companion object {
        private val PROTOCOLS_HANDLED = arrayOf("http://", "https://")

        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isBlank() && (PROTOCOLS_HANDLED.any { sourceUrl.startsWith(it) })
        }
    }
}