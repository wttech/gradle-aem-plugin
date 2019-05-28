package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.operation.FileDownloader
import com.cognifide.gradle.aem.common.file.operation.FileUploader
import com.cognifide.gradle.aem.common.utils.formats.JsonPassword
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.io.File
import java.io.IOException
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.*
import org.apache.http.client.utils.URIBuilder

class SftpTransfer(private val aem: AemExtension) : FileTransfer {

    var user: String? = aem.props.string("fileTransfer.sftp.user")

    @JsonSerialize(using = JsonPassword::class, `as` = String::class)
    var password: String? = aem.props.string("fileTransfer.sftp.password")

    var hostChecking = aem.props.boolean("fileTransfer.sftp.hostChecking") ?: false

    override fun download(url: String, name: String, target: File) {
        val uploadUrl = url.trimSlash()
        try {
            connect(uploadUrl) { path ->
                val remoteFile = open(fullPath(path, name), setOf(OpenMode.READ))
                val input = remoteFile.RemoteFileInputStream()

                FileDownloader(aem).download(remoteFile.length(), input, target)
            }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw FileException("Cannot download URL. File not found '$uploadUrl'.", e)
                else -> throw FileException("Cannot download URL '$uploadUrl' to file '$target' using SFTP. Cause: ${e.message}", e)
            }
        }
    }

    override fun upload(url: String, source: File) {
        val uploadUrl = url.trimSlash()
        try {
            connect(uploadUrl) { path ->
                open(fullPath(path, source.name), setOf(OpenMode.CREAT)).close()
                val remoteFile = open(fullPath(path, source.name), setOf(OpenMode.WRITE))
                val output = remoteFile.RemoteFileOutputStream()

                FileUploader(aem).upload(source, output)
            }
        } catch (e: SFTPException) {
            throw FileException("Cannot upload file '${source.path}' to URL '$uploadUrl' using SFTP: ${e.statusCode}, ${e.message}", e)
        }
    }

    override fun delete(url: String, name: String) {
        val uploadUrl = url.trimSlash()
        try {
            connect(uploadUrl) { path -> rm(fullPath(path, name)) }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw FileException("Cannot delete URL. File not found '$name'.", e)
                else -> throw FileException("Cannot delete file '$name' using SFTP: ${e.statusCode}, ${e.message}", e)
            }
        }
    }

    override fun list(url: String): List<String> {
        val uploadUrl = url.trimSlash()
        return connect(uploadUrl) { path -> ls(path).map { it.name } }
    }

    override fun truncate(url: String) {
        val uploadUrl = url.trimSlash()
        connect(uploadUrl) { path -> ls(path).forEach { rm(it.path) } }
    }

    override fun handles(url: String): Boolean {
        return !url.isBlank() && url.startsWith("sftp://")
    }

    private fun String.trimSlash() = trimEnd('/')

    private fun SFTPClient.isDirectory(path: String) = lstat(path).type == FileMode.Type.DIRECTORY

    private fun <T> connect(uploadUrl: String, action: SFTPClient.(path: String) -> T): T {
        validateUploadDir(uploadUrl)
        return connectSftp(uploadUrl, action)
    }

    private fun validateUploadDir(uploadUrl: String) {
        try {
            connectSftp(uploadUrl) { path ->
                if (!isDirectory(path)) {
                    throw AemException("uploadUrl must be a directory: '$uploadUrl'")
                }
            }
        } catch (e: SFTPException) {
            throw AemException("Problem accessing uploadUrl: '$uploadUrl': ${e.statusCode}, ${e.message}", e)
        }
    }

    @Suppress("MagicNumber")
    private fun <T> connectSftp(uploadUrl: String, action: SFTPClient.(path: String) -> T): T {
        val url = URIBuilder(uploadUrl)
        val ssh = SSHClient()

        ssh.loadKnownHosts()

        if (!hostChecking) {
            ssh.addHostKeyVerifier { _, _, _ -> true }
        }

        val user = if (!user.isNullOrBlank()) {
            user
        } else {
            url.userInfo
        }
        val port = if (url.port >= 0) {
            url.port
        } else {
            PORT_DEFAULT
        }

        ssh.connect(url.host, port)
        try {
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
        for ((_, method) in methods) {
            try {
                method()
                return
            } catch (e: IOException) {
                // intentionally empty
            }
        }
    }

    private fun fullPath(path: String, name: String) = "$path/$name"

    companion object {
        const val PORT_DEFAULT = 22
    }
}
