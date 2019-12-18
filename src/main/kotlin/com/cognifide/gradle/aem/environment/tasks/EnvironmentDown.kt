package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentDown : AemDefaultTask() {

    init {
        description = "Turns off AEM virtualized environment"
    }

    @TaskAction
    fun down() {
        if (!aem.environment.running) {
            logger.lifecycle("Environment cannot be turned off on as it is not running")
            return
        }

        aem.environment.down()

        aem.notifier.lifecycle("Environment down", "Turned off with success")
    }

    companion object {
        const val NAME = "environmentDown"
    }
}
