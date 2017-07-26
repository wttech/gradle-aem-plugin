package com.cognifide.gradle.aem.internal

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.net.URL

class UrlFileDownloader(val project: Project) {

    var user: String? = null

    var password: String? = null

    val logger: Logger = project.logger

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && Patterns.wildcard(sourceUrl, "*://*")
        }
    }

    fun download(url: String, file: File) {
        val connection = URL(url).openConnection()
        if (!user.isNullOrBlank() && !password.isNullOrBlank()) {
            connection.setRequestProperty("Authorization", "Basic ${Formats.toBase64("$user:$password")}")
        }
        connection.useCaches = false

        val downloader = ProgressFileDownloader(project)
        downloader.headerSourceTarget(url, file)
        downloader.size = connection.contentLengthLong

        downloader.download(connection.getInputStream(), file)
    }

}