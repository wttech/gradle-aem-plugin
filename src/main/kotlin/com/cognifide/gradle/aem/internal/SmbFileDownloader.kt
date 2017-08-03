package com.cognifide.gradle.aem.internal

import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import org.gradle.api.Project
import java.io.File

class SmbFileDownloader(val project: Project) {

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && sourceUrl.startsWith("smb://")
        }
    }

    var domain: String? = null

    var username: String? = null

    var password: String? = null

    fun download(sourceUrl: String, targetFile: File) {
        val smbFile = fileFor(sourceUrl)

        val downloader = ProgressFileDownloader(project)
        downloader.headerSourceTarget(sourceUrl, targetFile)
        downloader.size = smbFile.length()

        downloader.download(smbFile.inputStream, targetFile)
    }

    private fun fileFor(url: String): SmbFile {
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            return SmbFile(url, NtlmPasswordAuthentication(domain, username, password))
        }

        return SmbFile(url)
    }

}