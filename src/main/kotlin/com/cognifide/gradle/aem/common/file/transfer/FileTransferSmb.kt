package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.IoTransfer
import java.io.File
import java.io.IOException
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbException
import jcifs.smb.SmbFile

class FileTransferSmb(
    uploadUrl: String,
    private val credentials: Credentials,
    private val domain: String = "",
    private val ioTransfer: IoTransfer = IoTransfer()
) : FileTransfer {

    private val uploadUrl = if (uploadUrl.endsWith('/')) {
        uploadUrl
    } else {
        "$uploadUrl/"
    }

    init {
        if (!file(uploadUrl).isDirectory) {
            throw AemException("aem.backup.uploadUrl must be a directory: '$uploadUrl'")
        }
    }

    override fun download(name: String, target: File) {
        try {
            val smbFile = fileByName(name)
            if (!smbFile.exists()) {
                throw FileException("File not found $name")
            }
            ioTransfer.download(smbFile.length(), smbFile.inputStream, target)
        } catch (e: SmbException) {
            throw FileException("Cannot download URL '$name' to file '$target' using SMB. Cause: ${e.message}", e)
        }
    }

    override fun upload(source: File) {
        try {
            ioTransfer.upload(source, fileByName(source.name).outputStream)
        } catch (e: IOException) {
            throw FileException("Cannot upload file '${source.absolutePath}' to URL '$uploadUrl' using SMB. Cause: ${e.message}", e)
        }
    }

    override fun list(): List<String> {
        return uploadDir().listFiles().map { it.name }
    }

    override fun delete(name: String) {
        fileByName(name).delete()
    }

    override fun truncate() = uploadDir().listFiles().forEach { delete(it.name) }

    private fun file(url: String, name: String = "") =
            if (!credentials.username.isNullOrBlank() && !credentials.password.isNullOrBlank()) {
                SmbFile(url, name, NtlmPasswordAuthentication(domain, credentials.username, credentials.password))
            } else {
                SmbFile(url, name)
            }

    private fun fileByName(name: String) = file(uploadUrl, name)

    private fun uploadDir() = file(uploadUrl)

    companion object {
        fun handles(url: String): Boolean {
            return !url.isBlank() && url.startsWith("smb://")
        }
    }
}