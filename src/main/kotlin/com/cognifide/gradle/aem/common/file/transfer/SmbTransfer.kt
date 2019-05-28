package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.operation.FileDownloader
import com.cognifide.gradle.aem.common.file.operation.FileUploader
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.File
import java.io.IOException
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbException
import jcifs.smb.SmbFile

class SmbTransfer(private val aem: AemExtension) : FileTransfer {

    var domain: String? = aem.props.string("fileTransfer.smb.domain")

    var user: String? = aem.props.string("fileTransfer.smb.user")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var password: String? = aem.props.string("fileTransfer.smb.password")

    override fun download(url: String, name: String, target: File) {
        val uploadUrl = url.appendSlash()
        try {
            val smbFile = fileByName(uploadUrl, name)
            if (!smbFile.exists()) {
                throw FileException("File not found $name")
            }
            FileDownloader(aem).download(smbFile.length(), smbFile.inputStream, target)
        } catch (e: SmbException) {
            throw FileException("Cannot download URL '$name' to file '$target' using SMB. Cause: ${e.message}", e)
        }
    }

    override fun upload(url: String, source: File) {
        val uploadUrl = url.appendSlash()
        try {
            FileUploader(aem).upload(source, fileByName(uploadUrl, source.name).outputStream)
        } catch (e: IOException) {
            throw FileException("Cannot upload file '${source.absolutePath}' to URL '$uploadUrl' using SMB. Cause: ${e.message}", e)
        }
    }

    override fun list(url: String): List<String> {
        val uploadUrl = url.appendSlash()
        return uploadDir(uploadUrl).listFiles().map { it.name }
    }

    override fun delete(url: String, name: String) {
        val uploadUrl = url.appendSlash()
        fileByName(uploadUrl, name).delete()
    }

    override fun truncate(url: String) {
        val uploadUrl = url.appendSlash()
        uploadDir(uploadUrl).listFiles().forEach { delete(uploadUrl, it.name) }
    }

    override fun handles(url: String): Boolean {
        return !url.isBlank() && url.startsWith("smb://")
    }

    private fun file(uploadUrl: String, name: String = ""): SmbFile {
        validateUploadDir(uploadUrl)
        return smbFile(uploadUrl, name)
    }

    private fun smbFile(uploadUrl: String, name: String = ""): SmbFile {
        return if (!user.isNullOrBlank() && !password.isNullOrBlank()) {
            SmbFile(uploadUrl, name, NtlmPasswordAuthentication(domain, user, password))
        } else {
            SmbFile(uploadUrl, name)
        }
    }

    private fun fileByName(uploadUrl: String, name: String) = file(uploadUrl, name)

    private fun uploadDir(uploadUrl: String) = file(uploadUrl)

    private fun validateUploadDir(uploadUrl: String) {
        if (!smbFile(uploadUrl).isDirectory) {
            throw AemException("URL used for file upload must be a directory: '$uploadUrl'")
        }
    }

    private fun String.appendSlash() = if (this.endsWith('/')) {
        this
    } else {
        "$this/"
    }
}