package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.hosts.Hosts
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvHosts : AemDefaultTask() {

    init {
        description = "Append /etc/hosts file with specified list of hosts. " +
                "Requires super/admin user privileges."
    }

    @Internal
    private val hosts = Hosts.create(aem.environmentOptions.hosts.list)

    @TaskAction
    fun appendHosts() = hosts.append()

    companion object {
        const val NAME = "aemEnvHosts"
    }
}
