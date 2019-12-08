package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentHosts : AemDefaultTask() {

    init {
        description = "Prints environment hosts entries."
    }

    @TaskAction
    fun appendHosts() {
        logger.lifecycle("Hosts entries to be appended to ${aem.environment.hosts.osFile}:")
        logger.quiet(aem.environment.hosts.appendix)
    }

    companion object {
        const val NAME = "environmentHosts"
    }
}
