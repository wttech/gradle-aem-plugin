package com.cognifide.gradle.aem.config.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.tasks.InstanceCreate
import com.cognifide.gradle.aem.instance.tasks.InstanceSatisfy
import org.gradle.api.tasks.TaskAction

open class Resolve : AemDefaultTask() {

    init {
        description = "Resolve files from remote sources before running other tasks to optimize build time" +
                " and fail fast on configuration error."
    }

    @TaskAction
    fun resolve() {
        project.gradle.taskGraph.allTasks.forEach { task ->
            if (task is InstanceSatisfy) {
                logger.info("Resolving CRX packages for satisfying instances.")
                logger.info("Resolved CRX packages: ${task.allFiles}")
            } else if (task is InstanceCreate) {
                // TODO instance files eagerly jar & license file)
            }
        }
    }

    companion object {
        const val NAME = "aemResolve"
    }
}