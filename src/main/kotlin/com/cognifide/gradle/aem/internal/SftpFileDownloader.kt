package com.cognifide.gradle.aem.internal

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.apache.http.client.utils.URIBuilder
import org.gradle.api.Project
import java.io.File

// TODO still not working
class SftpFileDownloader(val project: Project) {

    companion object {
        fun handles(sourceUrl: String): Boolean {
            return !sourceUrl.isNullOrBlank() && sourceUrl.startsWith("sftp://")
        }
    }

    var username: String? = null

    var password: String? = null

    var knownHost: String? = null

    fun download(sourceUrl: String, targetFile: File) {
        try {
            val url = URIBuilder(sourceUrl)

            val downloader = ProgressFileDownloader(project)
            downloader.headerSourceTarget(sourceUrl, targetFile)

            connect(url, { _, channel ->
                downloader.size = channel.lstat(url.path).size
                downloader.download(channel.get(url.path), targetFile)
            })
        } catch (e: Exception) {
            throw DownloadException("Cannot download URL '$sourceUrl' to file '$targetFile' using SFTP. Check connection.", e)
        }
    }

    private fun connect(url: URIBuilder, action: (session: Session, channel: ChannelSftp) -> Unit) {
        val client = JSch()
        if (knownHost.isNullOrBlank()) {
            client.addIdentity("${System.getProperty("user.home")}/.ssh/id_rsa")
            client.setKnownHosts("${System.getProperty("user.home")}/.ssh/known_hosts")
        } else {
            client.setKnownHosts(knownHost!!.byteInputStream())
        }

        val session = if (!username.isNullOrBlank()) {
            client.getSession(username, url.host)
        } else {
            client.getSession(url.host)
        }

        if (!password.isNullOrBlank()) {
            session.setPassword(password)
        }

        session.connect()

        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect()

        action(session, channel)

        channel.exit()
        session.disconnect()
    }

}