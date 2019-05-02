package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentUp : AemDefaultTask() {

    init {
        description = "Turn on additional services for local environment " +
                "- based on provided docker compose file."
    }

    @TaskAction
    fun up() {
        if (aem.environment.running) {
            aem.notifier.notify("Environment up", "Cannot turn on as it is already running")
            return
        }

        aem.environment.up()
        aem.environment.check()

        aem.notifier.notify("Environment up", "Turned on with success. HTTP server / AEM dispatcher is now available")
    }

    companion object {
        const val NAME = "environmentUp"
    }
}
