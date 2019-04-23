package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.common.file.FileException
import java.io.File
import java.io.IOException
import java.io.OutputStream
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import org.gradle.api.Project

class SmbFileTransfer(val project: Project) : FileTransfer {

    var domain: String? = null

    var username: String? = null

    var password: String? = null

    override fun download(url: String, target: File) {
        try {
            project.logger.info("Downloading: $url -> ${target.absolutePath}")

            val smbFile = fileFor(url)

            val downloader = ProgressFileDownloader(project)
            downloader.size = smbFile.length()
            downloader.download(smbFile.inputStream, target)
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$url' to file '$target' using SMB. Cause: ${e.message}.", e)
        }
    }

    override fun upload(source: File, url: String) {
        try {
            val destination = fileFor(url)
            source.writeTo(destination.outputStream)
        } catch (e: IOException) {
            throw FileException("Cannot upload file '${source.absolutePath}' to URL '$url' using SMB. Cause: ${e.message}.", e)
        }
    }

    override fun delete(url: String) {
        fileFor(url).delete()
    }

    private fun fileFor(url: String): SmbFile {
        if (!handles(url)) {
            throw FileException("Cannot reference SMB file from '$url'")
        }
        if (!username.isNullOrBlank() && !password.isNullOrBlank()) {
            return SmbFile(url, NtlmPasswordAuthentication(domain, username, password))
        }
        return SmbFile(url)
    }

    private fun File.writeTo(outputStream: OutputStream) {
        this.inputStream().use { input ->
            outputStream.use { fileOut ->
                input.copyTo(fileOut)
            }
        }
    }

    companion object {
        fun handles(url: String): Boolean {
            return !url.isBlank() && url.startsWith("smb://")
        }
    }
}