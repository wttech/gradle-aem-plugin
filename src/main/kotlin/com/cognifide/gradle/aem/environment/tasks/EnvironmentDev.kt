package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import com.cognifide.gradle.aem.environment.reloader.Reloader
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvironmentDev : AemDefaultTask() {

    init {
        description = "Turns on environment development mode (interactive e.g HTTPD configuration reloading on file changes)"
    }

    @Internal
    val reloader = Reloader(aem.environment)

    @TaskAction
    fun dev() {
        if (!aem.environment.running) {
            aem.notifier.notify("Environment development mode", "Cannot turn on as environment is not running.")
            return
        }

        if (!reloader.configured) {
            aem.notifier.notify("Environment development mode", "None of containers have configured watched directory!")
            return
        }

        aem.progressLogger {
            // Whatever on parent logger to be able to pin children loggers from other threads
            progress("Watching files")

            reloader.start()
        }
    }

    companion object {
        const val NAME = "environmentDev"
    }
}
