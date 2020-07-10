package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.SlingDefaultTask
import org.gradle.api.tasks.TaskAction

open class InstanceResolve : SlingDefaultTask() {

    @TaskAction
    fun resolve() {
        sling.instanceManager.resolveFiles()
        sling.localInstanceManager.resolveFiles()
    }

    init {
        description = "Resolves instance files from remote sources before running other tasks"
    }

    companion object {
        const val NAME = "instanceResolve"
    }
}
