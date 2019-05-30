package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class FileMultiTransfer(private val aem: AemExtension) : FileTransfer {

    @JsonIgnore
    val factory = FileTransferFactory(aem)

    val http = HttpFileTransfer(aem)

    fun http(options: HttpFileTransfer.() -> Unit) {
        http.apply(options)
    }

    val sftp = SftpFileTransfer(aem)

    fun sftp(options: SftpFileTransfer.() -> Unit) {
        sftp.apply(options)
    }

    val smb = SmbFileTransfer(aem)

    fun smb(options: SmbFileTransfer.() -> Unit) {
        smb.apply(options)
    }

    val url = UrlFileTransfer(aem)

    fun url(options: UrlFileTransfer.() -> Unit) {
        url.apply(options)
    }

    val local = LocalFileTransfer(aem)

    fun local(options: LocalFileTransfer.() -> Unit) {
        local.apply(options)
    }

    private val custom = mutableListOf<CustomFileTransfer>()

    private val all = (custom + arrayOf(http, sftp, smb, url, local)).filter { it.enabled }

    override fun download(dirUrl: String, fileName: String, target: File) = handling(dirUrl).download(dirUrl, fileName, target)

    override fun upload(dirUrl: String, fileName: String, source: File) = handling(dirUrl).upload(dirUrl, fileName, source)

    override fun list(dirUrl: String): List<FileEntry> = handling(dirUrl).list(dirUrl)

    override fun delete(dirUrl: String, fileName: String) = handling(dirUrl).delete(dirUrl, fileName)

    override fun truncate(dirUrl: String) = handling(dirUrl).truncate(dirUrl)

    override fun handles(fileUrl: String) = all.any { it.handles(fileUrl) }

    fun handling(fileUrl: String): FileTransfer = all.find { it.handles(fileUrl) }
            ?: throw FileException("File transfer supporting URL '$fileUrl' not found!")

    fun named(name: String): FileTransfer {
        return all.find { it.name == name } ?: throw FileException("File transfer named '$name' not found!")
    }

    fun custom(name: String, definition: CustomFileTransfer.() -> Unit) {
        custom.add(CustomFileTransfer(aem).apply {
            this.name = name
            this.protocols = listOf("$name://*")

            apply(definition)
        })
    }

    @get:JsonIgnore
    override val name: String
        get() = NAME

    @get:JsonIgnore
    override val enabled: Boolean
        get() = true

    companion object {
        const val NAME = "multi"
    }
}