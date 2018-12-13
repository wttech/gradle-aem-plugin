package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.common.file.FileException
import java.io.File
import java.io.IOException
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import org.gradle.api.Project

class SmbFileDownloader(val project: Project) {

    var domain: String? = null

    var username: String? = null

    var password: String? = null

    fun download(sourceUrl: String, targetFile: File) {
        try {
            project.logger.info("Downloading: $sourceUrl -> ${targetFile.absolutePath}")

            val smbFile = fileFor(sourceUrl)

            val downloader = ProgressFileDownloader(project)
            downloader.size = smbFile.length()
            downloader.download(smbFile.inputStream, targetFile)
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using SMB. Check connection.", e)
        }
    }

    private fun fileFor(url: String): SmbFile {
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            return SmbFile(url, NtlmPasswordAuthentication(domain, username, password))
        }

        return SmbFile(url)
    }

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isBlank() && sourceUrl.startsWith("smb://")
        }
    }
}