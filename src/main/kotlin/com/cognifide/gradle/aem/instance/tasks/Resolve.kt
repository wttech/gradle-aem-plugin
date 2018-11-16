package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class Resolve : AemDefaultTask() {

    init {
        description = "Resolve files from remote sources before running other tasks to optimize build performance."
    }

    @get:Internal
    val satisfyTask
        get() = project.tasks.getByName(Satisfy.NAME) as Satisfy

    @get:Internal
    val createTask
        get() = project.tasks.getByName(Create.NAME) as Create

    @TaskAction
    fun resolve() {
        val premature = !willBeExecuted(Create.NAME) && !willBeExecuted(Satisfy.NAME)

        if (premature || willBeExecuted(Create.NAME)) {
            logger.info("Resolving instance files for creating instances.")
            logger.info("Resolved instance files: ${createTask.instanceFiles}")
        }
        if (premature || willBeExecuted(Satisfy.NAME)) {
            logger.info("Resolving CRX packages for satisfying instances.")
            logger.info("Resolved CRX packages: ${satisfyTask.allFiles}")
        }
    }

    companion object {
        const val NAME = "aemResolve"
    }

}