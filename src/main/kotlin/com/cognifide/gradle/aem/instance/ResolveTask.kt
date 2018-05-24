package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class ResolveTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemResolve"
    }

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
        val premature = !isTaskExecuted(CreateTask.NAME) && !isTaskExecuted(SatisfyTask.NAME)

        if (premature || isTaskExecuted(CreateTask.NAME)) {
            logger.info("Resolving instance files for creating instances.")
            logger.info("Resolved instance files: ${createTask.instanceFiles}")
        }
        if (premature || isTaskExecuted(SatisfyTask.NAME)) {
            logger.info("Resolving CRX packages for satisfying instances.")
            logger.info("Resolved CRX packages: ${satisfyTask.allFiles}")
        }
    }

    private fun isTaskExecuted(taskName: String): Boolean {
        return project.gradle.taskGraph.allTasks.any { it.name == taskName }
    }

}