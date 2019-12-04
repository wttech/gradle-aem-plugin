package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class InstanceResolve : AemDefaultTask() {

    init {
        description = "Resolves instance files from remote sources before running other tasks"
    }

    @TaskAction
    fun resolve() {
        aem.localInstanceManager.resolveSourceFiles()
        aem.tasks.instanceSatisfy.resolvePackages()
    }

    companion object {
        const val NAME = "instanceResolve"
    }
}
