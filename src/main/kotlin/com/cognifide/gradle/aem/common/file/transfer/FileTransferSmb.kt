package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.IoTransfer
import java.io.File
import java.io.IOException
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbException
import jcifs.smb.SmbFile

class FileTransferSmb(
    private val credentials: Credentials,
    private val domain: String = "",
    private val ioTransfer: IoTransfer = IoTransfer()
) : FileTransfer {

    override fun download(url: String, name: String, target: File) {
        val uploadUrl = url.appendSlash()
        try {
            val smbFile = fileByName(uploadUrl, name)
            if (!smbFile.exists()) {
                throw FileException("File not found $name")
            }
            ioTransfer.download(smbFile.length(), smbFile.inputStream, target)
        } catch (e: SmbException) {
            throw FileException("Cannot download URL '$name' to file '$target' using SMB. Cause: ${e.message}", e)
        }
    }

    override fun upload(url: String, source: File) {
        val uploadUrl = url.appendSlash()
        try {
            ioTransfer.upload(source, fileByName(uploadUrl, source.name).outputStream)
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

    private fun smbFile(uploadUrl: String, name: String = "") =
            if (!credentials.username.isNullOrBlank() && !credentials.password.isNullOrBlank()) {
                SmbFile(uploadUrl, name, NtlmPasswordAuthentication(domain, credentials.username, credentials.password))
            } else {
                SmbFile(uploadUrl, name)
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