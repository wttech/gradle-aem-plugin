package com.cognifide.gradle.aem.environment.hosts

import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.EnvironmentException
import java.io.File
import java.io.FileNotFoundException

class HostsAppenderWindows(
        private val hosts: HostsOptions
) : HostsAppender() {
    override fun appendHosts() = try {
        FileOperations.amendFile(File(hosts.file)) { appendedContent(it, hosts.list) }
    } catch (e: FileNotFoundException) {
        if (e.message?.contains("Access is denied") == true) {
            throw EnvironmentException("Failed to append hosts to ${hosts.file} file - permission denied." +
                    "\nThis very task requires admin privileges,  please run PowerShell 'As Administrator' and exec `.\\gradlew.bat aemEnvHosts`", e)
        }
        throw EnvironmentException("Failed to append hosts to ${hosts.file} file.", e)
    }
}