package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem

open class EnvironmentHosts : AemDefaultTask() {

    init {
        description = "Prints environment hosts entries."
    }

    private val hostsFile = when {
        OperatingSystem.current().isWindows -> """C:\Windows\System32\drivers\etc\hosts"""
        else -> "/etc/hosts"
    }

    @TaskAction
    fun appendHosts() {
        logger.lifecycle("Hosts entries to be appended to $hostsFile:")
        logger.quiet(aem.environment.hosts.defined.joinToString("\n") { it.text })
    }

    companion object {
        const val NAME = "environmentHosts"
    }
}
