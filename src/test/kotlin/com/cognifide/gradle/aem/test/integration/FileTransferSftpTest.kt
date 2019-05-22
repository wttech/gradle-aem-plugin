package com.cognifide.gradle.aem.test.integration

import com.cognifide.gradle.aem.common.file.transfer.Credentials
import com.cognifide.gradle.aem.common.file.transfer.FileTransferSftp
import org.junit.jupiter.api.Disabled

/**
 * Before running this integration test start docker image:
 * docker run -p 22:22 -d atmoz/sftp foo:pass:::upload
 */
@Disabled
class FileTransferSftpTest : FileTransferTest() {
    override val uploadUrl = "sftp://localhost/upload/"
    override val invalidUrl = "sftp://localhost/upload/file.txt"
    private val credentials = Credentials("foo", "pass")
    override fun transfer() = FileTransferSftp(credentials)
}
