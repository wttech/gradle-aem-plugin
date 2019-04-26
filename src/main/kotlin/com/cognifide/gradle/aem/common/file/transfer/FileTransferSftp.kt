package com.cognifide.gradle.aem.common.file.transfer

import com.cognifide.gradle.aem.common.AemException
import com.cognifide.gradle.aem.common.file.FileException
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.*
import net.schmizz.sshj.xfer.FileSystemFile
import org.apache.http.client.utils.URIBuilder

class FileTransferSftp(
    uploadUrl: String,
    private val credentials: Credentials,
    private val hostChecking: Boolean? = false
) : FileTransfer {

    private val uploadUrl = uploadUrl.trimEnd('/')

    init {
        try {
            connect { path ->
                if (!isDirectory(path)) {
                    throw AemException("aem.backup.uploadUrl must be a directory: '$uploadUrl'")
                }
            }
        } catch (e: SFTPException) {
            throw AemException("Problem accessing uploadUrl: '$uploadUrl': ${e.statusCode}, ${e.message}", e)
        }
    }

    @Suppress("MagicNumber")
    private fun File.writeFrom(inputStream: InputStream) {
        inputStream.use { input ->
            val output = FileOutputStream(this)
            var finished = false

            try {
                val buf = ByteArray(1024 * 10)
                var read = input.read(buf)

                while (read >= 0) {
                    output.write(buf, 0, read)
                    read = input.read(buf)
                }

                output.flush()
                finished = true
            } finally {
                output.close()
                if (!finished) {
                    this.delete()
                }
            }
        }
    }

    override fun download(name: String, target: File) {
        try {
            connect { path ->
                val input = open(fullPath(path, name), setOf(OpenMode.READ)).RemoteFileInputStream()
                target.writeFrom(input)
            }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw FileException("Cannot download URL. File not found '$uploadUrl'.", e)
                else -> throw FileException("Cannot download URL '$uploadUrl' to file '$target' using SFTP. Cause: ${e.message}", e)
            }
        }
    }

    override fun upload(source: File) {
        try {
            connect { path -> put(FileSystemFile(source), path) }
        } catch (e: SFTPException) {
            throw FileException("Cannot upload file '${source.path}' to URL '$uploadUrl' using SFTP: ${e.statusCode}, ${e.message}", e)
        }
    }

    override fun delete(name: String) {
        try {
            connect { path -> rm(fullPath(path, name)) }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw FileException("Cannot delete URL. File not found '$name'.", e)
                else -> throw FileException("Cannot delete file '$name' using SFTP: ${e.statusCode}, ${e.message}", e)
            }
        }
    }

    override fun list() = connect { path -> ls(path).map { it.name } }

    override fun truncate() = connect { path -> ls(path).forEach { rm(it.path) } }

    private fun SFTPClient.isDirectory(path: String) = lstat(path).type == FileMode.Type.DIRECTORY

    @Suppress("MagicNumber")
    private fun <T> connect(action: SFTPClient.(path: String) -> T): T {
        val url = URIBuilder(uploadUrl)
        val ssh = SSHClient()
        ssh.loadKnownHosts()
        if (hostChecking == null || !hostChecking) {
            ssh.addHostKeyVerifier { _, _, _ -> true }
        }

        val user = if (!credentials.username.isNullOrBlank()) credentials.username else url.userInfo
        val port = if (url.port >= 0) url.port else PORT_DEFAULT

        ssh.connect(url.host, port)
        try {
            authenticate(mapOf(
                    "public key" to { ssh.authPublickey(user) },
                    "password" to { ssh.authPassword(user, credentials.password) }
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
            }
        }
    }

    private fun fullPath(path: String, name: String) = "$path/$name"

    companion object {
        const val PORT_DEFAULT = 22
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isBlank() && sourceUrl.startsWith("sftp://")
        }
    }
}
