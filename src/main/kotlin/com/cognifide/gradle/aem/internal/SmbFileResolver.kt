package com.cognifide.gradle.aem.internal

import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import org.gradle.api.Project
import java.io.File

class SmbFileResolver(val project: Project, val auth: NtlmPasswordAuthentication? = null) {

    companion object {
        fun of(project: Project): SmbFileResolver {
            val props = PropertyParser(project)

            val domain = props.prop("aem.smb.domain")
            val username = props.prop("aem.smb.user")
            val password = props.prop("aem.smb.password")

            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                return SmbFileResolver(project, NtlmPasswordAuthentication(domain, username, password))
            } else {
                return SmbFileResolver(project, null)
            }
        }
    }

    fun download(sourceUrl: String, targetFile: File) {
        val smbFile = fileFor(sourceUrl)

        FileDownloader(project, sourceUrl, smbFile.length()).download(smbFile.inputStream, targetFile)
    }

    private fun fileFor(url: String): SmbFile {
        if (auth != null) {
            return SmbFile(url, auth)
        } else {
            return SmbFile(url)
        }
    }

}