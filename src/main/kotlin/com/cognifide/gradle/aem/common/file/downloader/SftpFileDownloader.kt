package com.cognifide.gradle.aem.common.file.downloader

import com.cognifide.gradle.aem.common.file.FileException
import com.cognifide.gradle.aem.common.file.IoTransferLogger
import java.io.File
import java.io.IOException
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.http.client.utils.URIBuilder
import org.gradle.api.Project
import org.gradle.api.logging.Logger

class SftpFileDownloader(val project: Project) {

    var username: String? = null

    var password: String? = null

    var hostChecking: Boolean = false

    val logger: Logger = project.logger

    fun download(sourceUrl: String, targetFile: File) {
        try {
            project.logger.info("Downloading: $sourceUrl -> ${targetFile.absolutePath}")

            val url = URIBuilder(sourceUrl)

            connect(url) { sftp ->
                val size = sftp.stat(url.path).size
                val input = sftp.open(url.path, setOf(OpenMode.READ)).RemoteFileInputStream()

                IoTransferLogger(project).download(size, input, targetFile)
            }
        } catch (e: IOException) {
            throw FileException("Cannot download URL '$sourceUrl' to file '$targetFile' using SFTP. Cause: ${e.message}", e)
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

        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isBlank() && sourceUrl.startsWith("sftp://")
        }
    }
}