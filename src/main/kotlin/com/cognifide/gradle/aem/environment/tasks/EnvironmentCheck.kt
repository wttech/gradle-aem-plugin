package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentCheck : AemDefaultTask() {

    init {
        description = "Checks virtualized AEM environment by running health checks."
    }

    @TaskAction
    fun check() {
        if (aem.offline) {
            aem.logger.info("Environment checking skipped as of offline mode is active.")
            return
        }

        aem.environment.check()
    }

    companion object {
        const val NAME = "environmentCheck"
    }
}
