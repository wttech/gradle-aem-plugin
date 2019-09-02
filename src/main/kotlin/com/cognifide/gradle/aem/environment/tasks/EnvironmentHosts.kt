package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.environment.hosts.Hosts
import org.gradle.api.tasks.TaskAction

open class EnvironmentHosts : AemDefaultTask() {

    init {
        description = "Append environment hosts to system hosts file. Requires super/admin user privileges."
    }

    @TaskAction
    fun appendHosts() {
        val hosts = Hosts.of(aem.environment.hosts.defined)
        hosts.append()

        aem.notifier.notify("Environment hosts", "Appended with success")
    }

    companion object {
        const val NAME = "environmentHosts"
    }
}
