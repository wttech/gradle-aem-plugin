package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import java.io.File

// TODO add http transfer and url transfer (with unsupported methods impl)
class FileMultiTransfer(aem: AemExtension) : FileTransfer {

    val sftp = SftpTransfer(aem)

    fun sftp(options: SftpTransfer.() -> Unit) {
        sftp.apply(options)
    }

    val smb = SmbTransfer(aem)

    fun smb(options: SmbTransfer.() -> Unit) {
        smb.apply(options)
    }

    private val all = arrayOf(sftp, smb)

    // TODO join url and name / split internally
    override fun download(url: String, name: String, target: File) = transfer(url).download(url, name, target)

    override fun upload(url: String, source: File) = transfer(url).upload(url, source)

    override fun list(url: String): List<String> = transfer(url).list(url)

    override fun delete(url: String, name: String) = transfer(url).delete(url, name)

    override fun truncate(url: String) = transfer(url).truncate(url)

    override fun handles(url: String) = all.any { it.handles(url) }

    private fun transfer(url: String) = all.find { it.handles(url) }
            ?: throw AemException("Invalid url for file transfer: $url. Only SMB and SFTP URLs are supported.")
}