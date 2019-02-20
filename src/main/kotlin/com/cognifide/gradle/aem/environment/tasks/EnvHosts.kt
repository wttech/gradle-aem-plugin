package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.hosts.HostsAppender
import java.io.File
import java.io.FileNotFoundException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvHosts : AemDefaultTask() {

    init {
        description = "Appends /etc/hosts file with specified list of hosts. " +
            "Requires super/admin user privileges."
    }

    @Internal
    private val appender = HostsAppender()

    @Input
    private val hosts = aem.environmentOptions.hosts

    @TaskAction
    fun appendHosts() = try {
        FileOperations.amendFile(File(hosts.file)) { appender.appendedContent(it, hosts.list) }
    } catch (e: FileNotFoundException) {
        if (e.message?.contains("Permission denied") == true) {
            throw EnvironmentException("Failed to append hosts to ${hosts.file} file - permission denied." +
                "\nConsider running this task with sudo or in PowerShell with admin privileges.", e)
        }
        throw EnvironmentException("Failed to append hosts to ${hosts.file} file.", e)
    }

    companion object {
        const val NAME = "aemEnvHosts"
    }
}
