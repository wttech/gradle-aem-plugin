package com.cognifide.gradle.aem.test.integration

import com.cognifide.gradle.aem.common.file.transfer.Credentials
import com.cognifide.gradle.aem.common.file.transfer.FileTransferSmb
import org.junit.jupiter.api.Disabled

/**
 * To run it provide URL and credentials for SMB server.
 */
@Disabled
class FileTransferSmbTest : FileTransferTest() {
    override val uploadUrl = "smb://server/path"
    override val invalidUrl = "smb://server/path/file.txt"
    private val credentials = Credentials("user", "password")
    override fun transfer() = FileTransferSmb(credentials)
}
