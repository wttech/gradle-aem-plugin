package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.transfer.generic.CustomFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.generic.LocalFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.generic.UrlFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class FileTransferManager(private val aem: AemExtension) : FileTransfer {

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

    /**
     * Downloads file of given name from directory at specified URL.
     */
    override fun downloadFrom(dirUrl: String, fileName: String, target: File) = handling(dirUrl).downloadFrom(dirUrl, fileName, target)

    /**
     * Uploads file to directory at specified URL and set given name.
     */
    override fun uploadTo(dirUrl: String, fileName: String, source: File) = handling(dirUrl).uploadTo(dirUrl, fileName, source)

    /**
     * Lists files in directory available at specified URL.
     */
    override fun list(dirUrl: String): List<FileEntry> = handling(dirUrl).list(dirUrl)

    /**
     * Deletes file of given name in directory at specified URL.
     */
    override fun deleteFrom(dirUrl: String, fileName: String) = handling(dirUrl).deleteFrom(dirUrl, fileName)

    /**
     * Deletes all files in directory available at specified URL.
     */
    override fun truncate(dirUrl: String) = handling(dirUrl).truncate(dirUrl)

    /**
     * Gets file status of given name in directory at specified URL.
     */
    override fun stat(dirUrl: String, fileName: String) = handling(dirUrl).stat(dirUrl, fileName)

    /**
     * Check if there is any file transfer supporting specified URL.
     */
    override fun handles(fileUrl: String) = all.any { it.handles(fileUrl) }

    /**
     * Find file transfer supporting specified URL.
     */
    fun handling(fileUrl: String): FileTransfer = all.find { it.handles(fileUrl) }
            ?: throw FileException("File transfer supporting URL '$fileUrl' not found!")

    /**
     * Register custom file transfer for e.g downloading / uploading files from cloud storages like:
     * Amazon S3, Google Cloud Storage etc.
     */
    fun custom(name: String, definition: CustomFileTransfer.() -> Unit) {
        custom.add(CustomFileTransfer(aem).apply {
            this.name = name
            this.protocols = listOf("$name://*")

            apply(definition)
        })
    }

    /**
     * Get custom (or built-in) file transfer by name.
     */
    fun named(name: String): FileTransfer {
        return all.find { it.name == name } ?: throw FileException("File transfer named '$name' not found!")
    }

    /**
     * Shorthand method to set same credentials for all protocols requiring it.
     *
     * Useful only in specific cases, when e.g company storage offers accessing files via multiple protocols
     * using same AD credentials.
     */
    fun credentials(user: String, password: String, domain: String? = null) {
        http.client.basicUser = user
        http.client.basicPassword = password

        sftp.user = user
        sftp.password = password

        smb.user = user
        smb.password = password
        smb.domain = domain
    }

    @get:JsonIgnore
    override val name: String
        get() = NAME

    @get:JsonIgnore
    override val enabled: Boolean
        get() = true

    companion object {
        const val NAME = "manager"
    }
}