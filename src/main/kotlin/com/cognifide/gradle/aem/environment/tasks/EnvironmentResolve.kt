package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class EnvironmentResolve : AemDefaultTask() {

    init {
        description = "Resolves environment files from remote sources before running other tasks"
    }

    @TaskAction
    fun resolve() {
        logger.info("Resolving environment distribution files")
        val files = aem.environment.resolve()
        logger.info("Resolved environment distribution files:\n${files.joinToString("\n")}")
    }

    companion object {
        const val NAME = "environmentResolve"
    }
}
