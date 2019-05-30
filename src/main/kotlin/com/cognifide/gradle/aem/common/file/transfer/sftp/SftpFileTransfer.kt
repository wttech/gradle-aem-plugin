package com.cognifide.gradle.aem.common.file.transfer.sftp

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.FileEntry
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.File
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

    override fun download(dirUrl: String, fileName: String, target: File) {
        connectDir(dirUrl) { path ->
            val filePath = "$path/$fileName"
            downloader().download(stat(filePath).size, read(filePath), target)
        }
    }

    override fun upload(dirUrl: String, fileName: String, source: File) {
        connectDir(dirUrl) { path ->
            val filePath = "$path/$fileName"
            uploader().upload(source, write(filePath))
        }
    }

    override fun list(dirUrl: String): List<FileEntry> {
        return connectDir(dirUrl) { path ->
            listDir(openDir(path))
                    .filter { !FILE_NAME_IGNORED.contains(it.filename) }
                    .map { FileEntry(it.filename, it.attributes.modifyTime.toMillis(), it.attributes.size)
            }
        }
    }

    override fun delete(dirUrl: String, fileName: String) {
        connectDir(dirUrl) { path ->
            val filePath = "$path/$fileName"
            remove(filePath)
        }
    }

    override fun truncate(dirUrl: String) {
        connectDir(dirUrl) { path ->
            listDir(openDir(path))
                    .filter { !FILE_NAME_IGNORED.contains(it.filename) }
                    .forEach { remove("$path/${it.filename}") }
        }
    }

    private fun <T> connectDir(dirUrl: String, callback: SftpClient.(String) -> T): T {
        return connect(dirUrl) { path ->
            if (!lstat(path).isDirectory) {
                throw SftpException("URL does not point to directory: '$dirUrl'")
            }
            callback(path)
        }
    }

    fun <T> connect(url: String, callback: SftpClient.(String) -> T): T {
        val urlConfig = URIBuilder(url)
        val user = if (!user.isNullOrBlank()) user else urlConfig.userInfo
        val port = if (urlConfig.port >= 0) urlConfig.port else PORT_DEFAULT
        val host = urlConfig.host

        SshClient.setUpDefaultClient().use { client ->
            client.apply(sshOptions)
            client.start()
            client.connect(user, host, port).apply { await(timeout) }.session.use { session ->
                session.apply(sessionOptions)
                session.addPasswordIdentity(password)
                session.auth().await(timeout)

                SftpClientFactory.instance().createSftpClient(session).use { sftp ->
                    return callback(sftp, urlConfig.path).also { client.stop() }
                }
            }
        }
    }

    companion object {
        const val NAME = "sftp"

        const val PORT_DEFAULT = 22

        val FILE_NAME_IGNORED = listOf(".", "..")
    }
}
