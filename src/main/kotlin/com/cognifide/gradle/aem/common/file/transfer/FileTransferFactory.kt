package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.http.HttpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.sftp.SftpFileTransfer
import com.cognifide.gradle.aem.common.file.transfer.smb.SmbFileTransfer

/**
 * Allows to create separate file transfer of same type but with different settings.
 *
 * Useful in task scripting to communicate with multiple remote servers.
 */
class FileTransferFactory(private val aem: AemExtension) {

    fun http(options: HttpFileTransfer.() -> Unit) = HttpFileTransfer(aem).apply(options)

    fun sftp(options: SftpFileTransfer.() -> Unit) = SftpFileTransfer(aem).apply(options)

    fun smb(options: SmbFileTransfer.() -> Unit) = SmbFileTransfer(aem).apply(options)
}