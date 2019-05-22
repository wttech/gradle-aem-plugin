package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.file.IoTransferLogger
import java.io.File

class FileTransferProtocolAgnostic(private val aem: AemExtension) : FileTransfer {

    private val fileTransfers by lazy {
        listOf(
                FileTransferSftp(
                        Credentials(aem.config.resolverOptions.sftpUsername, aem.config.resolverOptions.sftpPassword),
                        aem.config.resolverOptions.sftpHostChecking,
                        IoTransferLogger(aem.project)
                ),
                FileTransferSmb(
                        Credentials(aem.config.resolverOptions.smbUsername, aem.config.resolverOptions.smbPassword),
                        aem.config.resolverOptions.smbDomain ?: "",
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