package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class ResolveTask : DefaultTask() {

    companion object {
        val NAME = "aemResolve"
    }

    init {
        group = AemTask.GROUP
        description = "Resolve files from remote sources before running other tasks to optimize build performance."
    }

    @get:Internal
    val satisfyTask
        get() = project.tasks.getByName(SatisfyTask.NAME) as SatisfyTask

    @TaskAction
    fun resolve() {
        logger.info("Resolving packages to be satisfied")
        satisfyTask.packageGroups
    }

}