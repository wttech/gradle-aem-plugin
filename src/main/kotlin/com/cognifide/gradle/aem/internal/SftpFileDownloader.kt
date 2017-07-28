package com.cognifide.gradle.aem.internal

import org.gradle.api.Project
import java.io.File

class SftpFileDownloader(val project: Project) {

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && (sourceUrl.startsWith("ftp://") || sourceUrl.startsWith("sftp://"))
        }
    }

    var username: String? = null

    var password: String? = null

    // TODO implement sftp downloading
    fun download(sourceUrl: String, targetFile: File) {

        val downloader = ProgressFileDownloader(project)
        downloader.headerSourceTarget(sourceUrl, targetFile)
        downloader.size = 0 // smbFile.length()

        downloader.download(null!!, targetFile)
    }


}