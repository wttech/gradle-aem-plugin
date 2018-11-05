package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class ResolveTask : AemDefaultTask() {

    init {
        description = "Resolve files from remote sources before running other tasks to optimize build performance."
    }

    @get:Internal
    val satisfyTask
        get() = project.tasks.getByName(SatisfyTask.NAME) as SatisfyTask

    @get:Internal
    val createTask
        get() = project.tasks.getByName(CreateTask.NAME) as CreateTask

    @TaskAction
    fun resolve() {
        val premature = !willBeExecuted(CreateTask.NAME) && !willBeExecuted(SatisfyTask.NAME)

        if (premature || willBeExecuted(CreateTask.NAME)) {
            logger.info("Resolving instance files for creating instances.")
            logger.info("Resolved instance files: ${createTask.instanceFiles}")
        }
        if (premature || willBeExecuted(SatisfyTask.NAME)) {
            logger.info("Resolving CRX packages for satisfying instances.")
            logger.info("Resolved CRX packages: ${satisfyTask.allFiles}")
        }
    }

    companion object {
        const val NAME = "aemResolve"
    }

}