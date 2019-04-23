package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Resolve : AemDefaultTask() {

    init {
        description = "Resolve files from remote sources before running other tasks to optimize build time."
    }

    @get:Internal
    val satisfyTask
        get() = project.tasks.getByName(Satisfy.NAME) as Satisfy

    @TaskAction
    fun resolve() {
        val premature = !willBeExecuted(Satisfy.NAME)

        if (premature || willBeExecuted(Satisfy.NAME)) {
            logger.info("Resolving CRX packages for satisfying instances.")
            logger.info("Resolved CRX packages: ${satisfyTask.allFiles}")
        }
    }

    companion object {
        const val NAME = "aemResolve"
    }
}