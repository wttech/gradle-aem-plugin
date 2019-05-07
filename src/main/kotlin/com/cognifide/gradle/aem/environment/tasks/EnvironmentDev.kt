package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.docker.domain.HttpdReloader
import org.gradle.api.tasks.TaskAction

open class EnvironmentDev : AemDefaultTask() {

    init {
        description = "Watches for HTTPD configuration file changes and reloads service deployed on AEM virtualized environment"
    }

    val httpdReloader = HttpdReloader(aem)

    @TaskAction
    fun dev() {
        aem.progressLogger {
            // Whatever on parent logger to be able to pin children loggers from other threads
            progress("Watching files")

            httpdReloader.start()
        }
    }

    fun httpdReloader(options: HttpdReloader.() -> Unit) {
        httpdReloader.apply(options)
    }

    companion object {
        const val NAME = "environmentDev"
    }
}