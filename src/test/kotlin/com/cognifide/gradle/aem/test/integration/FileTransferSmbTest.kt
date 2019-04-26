package com.cognifide.gradle.aem.test.integration

import com.cognifide.gradle.aem.common.file.transfer.Credentials
import com.cognifide.gradle.aem.common.file.transfer.FileTransferSmb
import org.junit.jupiter.api.Disabled

/**
 * To run it provide URL and credentials for SMB server.
 */
@Disabled
class FileTransferSmbTest : FileTransferTest() {
    private val credentials = Credentials("user", "password")
    override fun transfer() = FileTransferSmb("smb://server/path", credentials)
    override fun invalidTransfer() = FileTransferSmb("smb://server/path/file.txt", credentials)
}
