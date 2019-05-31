package com.cognifide.gradle.aem.common.file.transfer.smb

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.File
import java.io.IOException
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbException
import jcifs.smb.SmbFile

class SmbFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    var domain: String? = aem.props.string("fileTransfer.smb.domain")

    var user: String? = aem.props.string("fileTransfer.smb.user")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var password: String? = aem.props.string("fileTransfer.smb.password")

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("smb://*")

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        val url = dirUrl.appendSlash()
        val fileUrl = "$dirUrl/$fileName"

        try {
            validateDir(dirUrl)

            val file = smbFile(url, fileName)
            if (!file.exists()) {
                throw SmbFileException("Cannot download URL '$fileUrl'. File not found!")
            }

            downloader().download(file.length(), file.inputStream, target)
        } catch (e: SmbException) {
            throw SmbFileException("Cannot download URL '$fileUrl' to file '$target'. Cause: ${e.message}", e)
        }
    }

    override fun uploadTo(dirUrl: String, fileName: String, source: File) {
        val url = dirUrl.appendSlash()
        try {
            validateDir(url)
            uploader().upload(source, smbFile(url, fileName).outputStream)
        } catch (e: IOException) {
            throw SmbFileException("Cannot upload file '$source' to URL '$url'. Cause: ${e.message}", e)
        }
    }

    override fun list(dirUrl: String): List<FileEntry> {
        val url = dirUrl.appendSlash()
        try {
            validateDir(url)
            return smbFile(url).listFiles().map { FileEntry(it.name, it.lastModified(), it.length()) }
        } catch (e: IOException) {
            throw SmbFileException("Cannot list files at URL '$url'. Cause: ${e.message}", e)
        }
    }

    override fun deleteFrom(dirUrl: String, fileName: String) {
        val url = dirUrl.appendSlash()
        try {
            validateDir(dirUrl)
            smbFile(url, fileName).delete()
        } catch (e: IOException) {
            throw SmbFileException("Cannot delete files at URL '$url'. Cause: ${e.message}", e)
        }
    }

    override fun truncate(dirUrl: String) {
        val url = dirUrl.appendSlash()
        try {
            validateDir(url)
            smbFile(url).listFiles().forEach {
                deleteFrom(url, it.name)
            }
        } catch (e: IOException) {
            throw SmbFileException("Cannot truncate files at URL '$url'. Cause: ${e.message}", e)
        }
    }

    private fun smbFile(dirUrl: String, fileName: String = ""): SmbFile {
        return if (!user.isNullOrBlank() && !password.isNullOrBlank()) {
            SmbFile(dirUrl, fileName, NtlmPasswordAuthentication(domain, user, password))
        } else {
            SmbFile(dirUrl, fileName)
        }
    }

    private fun validateDir(url: String) {
        if (!smbFile(url).isDirectory) {
            throw SmbFileException("URL does not point to directory: '$url'")
        }
    }

    // TODO fun <T> connectDir() // same as in sftp

    private fun String.appendSlash() = if (this.endsWith('/')) {
        this
    } else {
        "$this/"
    }

    companion object {
        const val NAME = "smb"
    }
}