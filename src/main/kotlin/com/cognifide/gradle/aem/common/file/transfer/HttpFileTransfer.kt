package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.http.HttpClient
import java.io.File
import java.io.IOException

class HttpFileTransfer(aem: AemExtension, val client: HttpClient = client(aem)) : ProtocolFileTransfer(aem) {

    fun client(configurer: HttpClient.() -> Unit) {
        client.apply(configurer)
    }

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("http://*", "https://*")

    override fun download(dirUrl: String, fileName: String, target: File) {
        val sourceUrl = "$dirUrl/$fileName"

        try {
            client.get(sourceUrl) { response ->
                aem.logger.info("Downloading: $sourceUrl -> $target")
                downloader().download(response.entity.contentLength, asStream(response), target)
            }
        } catch (e: AemException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$target' using HTTP(s). Cause: ${e.message}", e)
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$target' using HTTP(s). Cause: ${e.message}", e)
        }
    }

    companion object {
        const val NAME = "httpd"

        fun client(aem: AemExtension) = HttpClient(aem).apply {
            basicUser = aem.props.string("fileTransfer.http.user") ?: ""
            basicPassword = aem.props.string("fileTransfer.http.password") ?: ""
            connectionIgnoreSsl = aem.props.boolean("fileTransfer.http.connectionIgnoreSsl") ?: true
        }
    }
}