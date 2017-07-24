package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.internal.PropertyParser
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import org.gradle.api.Project
import java.io.File
import java.io.FileOutputStream

class SmbFileResolver(val auth: NtlmPasswordAuthentication? = null) {

    companion object {
        fun of(project: Project): SmbFileResolver {
            val props = PropertyParser(project)

            val domain = props.prop("aem.smb.domain")
            val username = props.prop("aem.smb.user")
            val password = props.prop("aem.smb.password")

            if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
                return SmbFileResolver(NtlmPasswordAuthentication(domain, username, password))
            } else {
                return SmbFileResolver(null)
            }
        }
    }

    fun download(sourceUrl: String, targetFile: File) {
        fileFor(sourceUrl).inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun fileFor(url: String): SmbFile {
        if (auth != null) {
            return SmbFile(url, auth)
        } else {
            return SmbFile(url)
        }
    }

}