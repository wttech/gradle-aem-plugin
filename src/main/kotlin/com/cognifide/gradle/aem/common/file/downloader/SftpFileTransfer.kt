package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.common.file.FileException
import java.io.File
import java.io.IOException
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.Response
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPException
import net.schmizz.sshj.xfer.FileSystemFile
import org.apache.http.client.utils.URIBuilder
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class SftpFileTransfer(val project: Project) : FileTransfer {

    var username: String? = null

    var password: String? = null

    var hostChecking: Boolean = false

    val logger: Logger = project.logger

    override fun download(url: String, target: File) {
        try {
            project.logger.info("Downloading: $url -> ${target.absolutePath}")

            val urlObj = URIBuilder(url)
            val downloader = ProgressFileDownloader(project)

            connect(urlObj) { sftp ->
                val size = sftp.stat(urlObj.path).size
                val input = sftp.open(urlObj.path, setOf(OpenMode.READ)).RemoteFileInputStream()

                downloader.size = size
                downloader.download(input, target)
            }
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$url' to file '$target' using SFTP. Cause: ${e.message}", e)
        }
    }

    override fun upload(source: File, url: String) {
        try {
            val urlObj = URIBuilder(url)
            connect(urlObj) { sftp ->
                sftp.put(FileSystemFile(source), urlObj.path)
            }
        } catch (e: SFTPException) {
            throw FileException("Cannot upload file '${source.path}' to URL '$url' using SFTP: ${e.statusCode}, ${e.message}", e)
        }
    }

    override fun delete(url: String) {
        try {
            val urlObj = URIBuilder(url)
            connect(urlObj) { sftp ->
                sftp.rm(urlObj.path)
            }
        } catch (e: SFTPException) {
            when (e.statusCode) {
                Response.StatusCode.NO_SUCH_FILE -> throw FileException("Cannot delete URL. File not found '$url'.", e)
                else -> throw FileException("Cannot delete file '$url' using SFTP: ${e.statusCode}, ${e.message}", e)
            }
        }
    }

    private fun connect(url: URIBuilder, action: (SFTPClient) -> Unit) {
        val ssh = SSHClient()
        ssh.loadKnownHosts()
        if (!hostChecking) {
            ssh.addHostKeyVerifier { _, _, _ -> true }
        }

        val user = if (!username.isNullOrBlank()) username else url.userInfo
        val port = if (url.port >= 0) url.port else PORT_DEFAULT

        ssh.connect(url.host, port)
        try {
            authenticate(mapOf(
                    "public key" to { ssh.authPublickey(user) },
                    "password" to { ssh.authPassword(user, password) }
            ))
            ssh.newSFTPClient().use(action)
        } finally {
            ssh.disconnect()
        }
    }

    private fun authenticate(methods: Map<String, () -> Unit>) {
        for ((name, method) in methods) {
            try {
                method()
                logger.info("Authenticated using method: $name")
                return
            } catch (e: IOException) {
                logger.debug("Cannot authenticate using method: $name", e)
            }
        }
    }

    companion object {
        const val PORT_DEFAULT = 22

        fun handles(url: String): Boolean {
            return !url.isBlank() && url.startsWith("sftp://")
        }
    }
}