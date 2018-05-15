package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class ResolveTask : AemDefaultTask() {

    companion object {
        val NAME = "aemResolve"
    }

    init {
        description = "Resolve files from remote sources before running other tasks to optimize build performance."
    }

    @get:Internal
    val satisfyTask
        get() = project.tasks.getByName(SatisfyTask.NAME) as SatisfyTask

    @TaskAction
    fun resolve() {
        satisfyTask.packageGroups
    }

}