package com.cognifide.gradle.aem.common.file.transfer.sftp

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.transfer.ProtocolFileTransfer
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.File
import java.io.IOException
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.*
import org.apache.http.client.utils.URIBuilder

class SftpFileTransfer(aem: AemExtension) : ProtocolFileTransfer(aem) {

    var user: String? = aem.props.string("fileTransfer.sftp.user")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var password: String? = aem.props.string("fileTransfer.sftp.password")

    var hostChecking: Boolean = aem.props.boolean("fileTransfer.sftp.hostChecking") ?: false

    override val name: String
        get() = NAME

    override val protocols: List<String>
        get() = listOf("sftp://*")

    override fun download(dirUrl: String, fileName: String, target: File) {
        val url = dirUrl.trimSlash()
        try {
            connect(url) { path ->
                val remoteFile = open(fullPath(path, fileName), setOf(OpenMode.READ))
                val input = remoteFile.RemoteFileInputStream()

                downloader().download(remoteFile.length(), input, target)
            }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw SftpException("Cannot download URL. File not found '$url'.", e)
                else -> throw SftpException("Cannot download URL '$url' to file '$target'. Cause: ${e.message}", e)
            }
        }
    }

    override fun upload(dirUrl: String, fileName: String, source: File) {
        val url = dirUrl.trimSlash()
        try {
            connect(url) { path ->
                validateDir(path, url)

                open(fullPath(path, fileName), setOf(OpenMode.CREAT)).close()
                val remoteFile = open(fullPath(path, fileName), setOf(OpenMode.WRITE))
                val output = remoteFile.RemoteFileOutputStream()

                uploader().upload(source, output)
            }
        } catch (e: SFTPException) {
            throw SftpException("Cannot upload file '$source' to URL '$url'. Cause: ${e.message}", e)
        }
    }

    override fun delete(dirUrl: String, fileName: String) {
        val url = dirUrl.trimSlash()
        try {
            connect(url) { path ->
                rm(fullPath(path, fileName))
            }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw SftpException("Cannot delete URL. File not found '$fileName'.", e)
                else -> throw SftpException("Cannot delete file '$fileName'. Cause: ${e.message}", e)
            }
        }
    }

    override fun list(dirUrl: String): List<String> {
        val uploadUrl = dirUrl.trimSlash()
        return connect(uploadUrl) { path ->
            validateDir(path, dirUrl)
            ls(path).map { it.name }
        }
    }

    override fun truncate(dirUrl: String) {
        val uploadUrl = dirUrl.trimSlash()
        connect(uploadUrl) { path ->
            validateDir(path, dirUrl)
            ls(path).forEach { rm(it.path) }
        }
    }

    private fun String.trimSlash() = trimEnd('/')

    private fun SFTPClient.validateDir(path: String, dirUrl: String) {
        if (lstat(path).type != FileMode.Type.DIRECTORY) {
            throw AemException("URL is not pointing to directory: '$dirUrl'")
        }
    }

    @Suppress("MagicNumber")
    private fun <T> connect(uploadUrl: String, action: SFTPClient.(path: String) -> T): T {
        val url = URIBuilder(uploadUrl)
        val ssh = SSHClient()

        ssh.loadKnownHosts()

        if (!hostChecking) {
            ssh.addHostKeyVerifier { _, _, _ -> true }
        }

        val user = if (!user.isNullOrBlank()) user else url.userInfo
        val port = if (url.port >= 0) url.port else PORT_DEFAULT

        try {
            ssh.connect(url.host, port)
            authenticate(mapOf(
                    "public key" to { ssh.authPublickey(user) },
                    "password" to { ssh.authPassword(user, password) }
            ))
            return ssh.newSFTPClient().use { it.action(url.path) }
        } finally {
            ssh.disconnect()
        }
    }

    @Suppress("EmptyCatchBlock")
    private fun authenticate(methods: Map<String, () -> Unit>) {
        for ((name, method) in methods) {
            try {
                method()
                return
            } catch (e: IOException) {
                aem.logger.debug("Cannot authenticate SFTP using method '$name'", e)
            }
        }
    }

    private fun fullPath(path: String, name: String) = "$path/$name"

    companion object {
        const val NAME = "sftp"

        const val PORT_DEFAULT = 22
    }
}
