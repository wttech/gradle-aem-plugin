package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class InstanceResolve : AemDefaultTask() {

    @TaskAction
    fun resolve() {
        aem.localInstanceManager.resolveFiles()
    }

    init {
        description = "Resolves instance files from remote sources before running other tasks"
    }

    companion object {
        const val NAME = "instanceResolve"
    }
}
