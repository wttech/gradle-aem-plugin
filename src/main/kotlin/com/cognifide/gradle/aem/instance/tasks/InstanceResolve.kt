package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.instance.satisfy.InstanceSatisfy
import org.gradle.api.tasks.TaskAction

open class InstanceResolve : LocalInstanceTask() {

    init {
        description = "Resolves instance files from remote sources before running other tasks"
    }

    @TaskAction
    fun resolve() {
        common.tasks.get<InstanceSatisfy>(InstanceSatisfy.NAME).resolvePackages() // more light at first
        localInstanceManager.resolveSourceFiles()
    }

    companion object {
        const val NAME = "instanceResolve"
    }
}
