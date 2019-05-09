package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentCheck : AemDefaultTask() {

    init {
        description = "Checks virtualized AEM environment by running health checks."
    }

    @TaskAction
    fun check() {
        aem.environment.check()
    }

    companion object {
        const val NAME = "environmentCheck"
    }
}
