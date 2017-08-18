package com.cognifide.gradle.aem.internal.file

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.OpenMode
import net.schmizz.sshj.sftp.SFTPClient
import org.apache.http.client.utils.URIBuilder
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File

class SftpFileDownloader(val project: Project) {

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && sourceUrl.startsWith("sftp://")
        }
    }

    var username: String? = null

    var password: String? = null

    var hostChecking: Boolean = false

    val logger: Logger = project.logger

    fun download(sourceUrl: String, targetFile: File) {
        try {
            val url = URIBuilder(sourceUrl)

            val downloader = ProgressFileDownloader(project)
            downloader.headerSourceTarget(sourceUrl, targetFile)

            connect(url, { sftp ->
                val size = sftp.stat(url.path).size
                val input = sftp.open(url.path, setOf(OpenMode.READ)).RemoteFileInputStream()

                downloader.size = size
                downloader.download(input, targetFile)
            })
        } catch (e: Exception) {
            throw DownloadException("Cannot download URL '$sourceUrl' to file '$targetFile' using SFTP. Check connection.", e)
        }
    }

    private fun connect(url: URIBuilder, action: (SFTPClient) -> Unit) {
        val ssh = SSHClient()
        ssh.loadKnownHosts()
        if (!hostChecking) {
            ssh.addHostKeyVerifier({ _, _, _ -> true })
        }

        val user = if (!username.isNullOrBlank()) username else url.userInfo
        val port = if (url.port >= 0) url.port else 22

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
            } catch (e: Exception) {
                logger.debug("Cannot authenticate using method: $name", e)
            }
        }
    }

}