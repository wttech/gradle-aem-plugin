package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.environment.docker.domain.HttpdReloader
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvironmentDev : AemDefaultTask() {

    init {
        description = "Turns on environment development mode (interactive HTTPD configuration reloading on file changes)"
    }

    @Internal
    val httpdReloader = HttpdReloader(aem)

    @TaskAction
    fun dev() {
        if (!aem.environment.running) {
            aem.notifier.notify("Environment development mode", "Cannot turn on as environment is not running.")
            return
        }

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