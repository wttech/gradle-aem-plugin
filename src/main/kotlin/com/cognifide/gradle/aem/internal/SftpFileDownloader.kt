package com.cognifide.gradle.aem.internal

import net.schmizz.sshj.DefaultConfig
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.LoggerFactory
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
                val input = sftp.open(url.path, setOf(OpenMode.READ)).RemoteFileInputStream()

                downloader.download(input, targetFile)
            })
        } catch (e: Exception) {
            throw DownloadException("Cannot download URL '$sourceUrl' to file '$targetFile' using SFTP. Check connection.", e)
        }
    }

    private fun connect(url: URIBuilder, callback: (SFTPClient) -> Unit) {
        val config = DefaultConfig()
        config.loggerFactory = object : LoggerFactory {
            override fun getLogger(name: String?): Logger {
                return logger
            }

            override fun getLogger(clazz: Class<*>?): Logger {
                return logger
            }
        }

        val ssh = SSHClient(config)
        if (!hostChecking) {
            ssh.addHostKeyVerifier({ _, _, _ -> true })
        }
        ssh.loadKnownHosts()

        val user = if (!username.isNullOrBlank()) username else url.userInfo
        val port = if (url.port >= 0) url.port else 22

        ssh.connect(url.host, port)
        try {
            try {
                ssh.authPublickey(user)
            } catch (e: Exception) {
                logger.debug("Cannot authenticate using public key", e)

                try {
                    ssh.authPassword(user, password)
                } catch (e: Exception) {
                    logger.debug("Cannot authenticate using password", e)
                }
            }

            val sftp = ssh.newSFTPClient()
            sftp.use(callback)
        } finally {
            ssh.disconnect()
        }
    }

}