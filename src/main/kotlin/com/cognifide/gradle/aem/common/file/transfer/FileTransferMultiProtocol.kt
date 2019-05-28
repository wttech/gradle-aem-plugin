package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.IoTransferLogger
import java.io.File

class FileTransferMultiProtocol(private val aem: AemExtension) : FileTransfer {

    private val fileTransfers by lazy {
        listOf(
                FileTransferSftp(
                        Credentials(aem.resolverOptions.sftpUsername, aem.resolverOptions.sftpPassword),
                        aem.resolverOptions.sftpHostChecking,
                        IoTransferLogger(aem.project)
                ),
                FileTransferSmb(
                        Credentials(aem.resolverOptions.smbUsername, aem.resolverOptions.smbPassword),
                        aem.resolverOptions.smbDomain ?: "",
                        IoTransferLogger(aem.project)
                )
        )
    }

    override fun download(url: String, name: String, target: File) = fileTransfer(url).download(url, name, target)

    override fun upload(url: String, source: File) = fileTransfer(url).upload(url, source)

    override fun list(url: String): List<String> = fileTransfer(url).list(url)

    override fun delete(url: String, name: String) = fileTransfer(url).delete(url, name)

    override fun truncate(url: String) = fileTransfer(url).truncate(url)

    override fun handles(url: String) = fileTransfers.any { it.handles(url) }

    private fun fileTransfer(url: String) = fileTransfers.find { it.handles(url) }
            ?: throw AemException("Invalid url for file transfer: $url. Only SMB and SFTP URLs are supported.")
}