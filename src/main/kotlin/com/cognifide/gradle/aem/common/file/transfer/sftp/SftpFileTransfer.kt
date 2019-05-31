package com.cognifide.gradle.aem.common.file.transfer.sftp

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.File
import java.io.IOException
import org.apache.http.client.utils.URIBuilder
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.client.subsystem.sftp.SftpClient
import org.apache.sshd.client.subsystem.sftp.SftpClientFactory

class SftpFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    var user: String? = aem.props.string("fileTransfer.sftp.user")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var password: String? = aem.props.string("fileTransfer.sftp.password")

    var timeout: Long = aem.props.long("fileTransfer.sftp.timeout") ?: 60000L

    @JsonIgnore
    var sshOptions: SshClient.() -> Unit = {}

    fun ssh(options: SshClient.() -> Unit) {
        this.sshOptions = options
    }

    @JsonIgnore
    var sessionOptions: ClientSession.() -> Unit = {}

    fun session(options: ClientSession.() -> Unit) {
        this.sessionOptions = options
    }

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("sftp://*")

    override fun downloadFrom(dirUrl: String, fileName: String, target: File) {
        connectDir(dirUrl) { dirPath ->
            try {
                val filePath = "$dirPath/$fileName"
                downloader().download(stat(filePath).size, read(filePath), target)
            } catch (e: IOException) {
                throw SftpFileException("Cannot download file from URL '$dirUrl/$fileName'")
            }
        }
    }

    override fun uploadTo(dirUrl: String, fileName: String, source: File) {
        connectDir(dirUrl) { dirPath ->
            try {
                val filePath = "$dirPath/$fileName"
                uploader().upload(source, write(filePath))
            } catch (e: IOException) {
                throw SftpFileException("Cannot upload file '$source' to URL '$dirUrl/$fileName'", e)
            }
        }
    }

    override fun list(dirUrl: String): List<FileEntry> {
        return connectDir(dirUrl) { dirPath ->
            try {
                listDir(openDir(dirPath))
                        .filter { it.attributes.isRegularFile }
                        .map { FileEntry(it.filename, it.attributes.modifyTime.toMillis(), it.attributes.size) }
            } catch (e: IOException) {
                throw SftpFileException("Cannot list files in directory at URL '$dirUrl'", e)
            }
        }
    }

    override fun deleteFrom(dirUrl: String, fileName: String) {
        connectDir(dirUrl) { dirPath ->
            val filePath = "$dirPath/$fileName"
            remove(filePath)
        }
    }

    override fun truncate(dirUrl: String) {
        connectDir(dirUrl) { dirPath ->
            try {
                listDir(openDir(dirPath))
                        .filter { it.attributes.isRegularFile }
                        .forEach { remove("$dirPath/${it.filename}") }
            } catch (e: IOException) {
                throw SftpFileException("Cannot truncate directory at URL '$dirUrl'", e)
            }
        }
    }

    private fun <T> connectDir(dirUrl: String, callback: SftpClient.(String) -> T): T {
        return connect(dirUrl) { dirPath ->
            try {
                if (!lstat(dirPath).isDirectory) {
                    throw SftpFileException("Path at URL '$dirUrl' is not a directory.")
                }
            } catch (e: IOException) {
                throw SftpFileException("Directory at URL '$dirUrl' does not exist.", e)
            }

            callback(dirPath)
        }
    }

    @Suppress("ComplexMethod", "NestedBlockDepth")
    fun <T> connect(url: String, callback: SftpClient.(String) -> T): T {
        val urlConfig = URIBuilder(url)
        val userInfo = urlConfig.userInfo?.split(":") ?: listOf()
        val user = userInfo.takeIf { it.isNotEmpty() }?.get(0) ?: user
        val password = userInfo.takeIf { it.size == 2 }?.get(1) ?: password
        val port = if (urlConfig.port >= 0) urlConfig.port else PORT_DEFAULT
        val host = urlConfig.host

        try {
            SshClient.setUpDefaultClient().use { client ->
                client.apply(sshOptions)
                client.start()
                client.connect(user, host, port).apply { await(timeout) }.session.use { session ->
                    session.apply(sessionOptions)
                    if (!password.isNullOrBlank()) {
                        session.addPasswordIdentity(password)
                    }
                    session.auth().await(timeout)

                    SftpClientFactory.instance().createSftpClient(session).use { sftp ->
                        return callback(sftp, urlConfig.path).also { client.stop() }
                    }
                }
            }
        } catch (e: IOException) {
            throw SftpFileException("SFTP file transfer error (check credentials, network / VPN etc)", e)
        }
    }

    companion object {
        const val NAME = "sftp"

        const val PORT_DEFAULT = 22
    }
}
