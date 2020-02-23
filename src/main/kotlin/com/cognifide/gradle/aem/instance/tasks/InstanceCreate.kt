package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceCreate : LocalInstanceTask() {

    @TaskAction
    fun create() {
        val createdInstances = localInstanceManager.create(instances.get())
        common.notifier.lifecycle("Instance(s) created", "Which: ${createdInstances.names}")
    }

    init {
        description = "Creates local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceCreate"
    }
}
