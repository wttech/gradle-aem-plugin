package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.EnvironmentException
import java.io.File
import java.io.FileNotFoundException

class HostsAppenderUnix(
        private val hosts: HostsOptions
) : HostsAppender() {
    override fun appendHosts() = try {
        FileOperations.amendFile(File(hosts.file)) { appendedContent(it, hosts.list) }
    } catch (e: FileNotFoundException) {
        if (e.message?.contains("Permission denied") == true) {
            throw EnvironmentException("Failed to append hosts to ${hosts.file} file - permission denied." +
                    "\nThis very task requires super user privileges: `sudo ./gradlew aemEnvHosts`", e)
        }
        throw EnvironmentException("Failed to append hosts to ${hosts.file} file.", e)
    }
}