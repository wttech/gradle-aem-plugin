package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.hosts.HostsAppender
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvHosts : AemDefaultTask() {

    init {
        description = "Appends /etc/hosts file with specified list of hosts. " +
            "Requires super/admin user privileges."
    }

    @Internal
    private val appender = HostsAppender.create(aem.environmentOptions.hosts)

    @Input
    private val hosts = aem.environmentOptions.hosts

    @TaskAction
    fun appendHosts() = appender.appendHosts()

    companion object {
        const val NAME = "aemEnvHosts"
    }
}
