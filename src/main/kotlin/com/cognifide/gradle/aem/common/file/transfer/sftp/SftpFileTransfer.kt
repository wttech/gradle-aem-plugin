package com.cognifide.gradle.aem.common.file.transfer.sftp

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.apache.http.client.utils.URIBuilder
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.client.subsystem.sftp.SftpClient
import org.apache.sshd.client.subsystem.sftp.SftpClientFactory
import java.io.File
import java.net.URL

class SftpFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    var user: String? = aem.props.string("fileTransfer.sftp.user")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var password: String? = aem.props.string("fileTransfer.sftp.password")

    var timeout: Long = aem.props.long("fileTransfer.sftp.timeout") ?: Long.MAX_VALUE

    var sshOptions: SshClient.() -> Unit = {}

    fun ssh(options: SshClient.() -> Unit) {
        this.sshOptions = options
    }

    var sessionOptions: ClientSession.() -> Unit = {}

    fun session(options: ClientSession.() -> Unit) {
        this.sessionOptions = options
    }

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("sftp://*")

    override fun download(dirUrl: String, fileName: String, target: File) {
        connect(dirUrl) { path ->
            val filePath = "$path/$fileName"
            downloader().download(stat(filePath).size, read(filePath), target)
        }
    }

    override fun upload(dirUrl: String, fileName: String, source: File) {
        connect(dirUrl) { path ->
            val filePath = "$path/$fileName"
            uploader().upload(source, write(filePath))
        }
    }

    override fun list(dirUrl: String): List<String> {
        return connect(dirUrl) { path ->
            listDir(openDir(path)).map { it.filename }
        }
    }

    override fun delete(dirUrl: String, fileName: String) {
        connect(dirUrl) { path ->
            val filePath = "$path/$fileName"
            remove(filePath)
        }
    }

    override fun truncate(dirUrl: String) {
        connect(dirUrl) { path ->
            listDir(openDir(path)).forEach { remove("$path/${it.filename}") }
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
    }
}
