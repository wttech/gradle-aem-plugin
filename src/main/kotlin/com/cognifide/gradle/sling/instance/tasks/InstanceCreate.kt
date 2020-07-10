package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.*
import com.cognifide.gradle.sling.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceCreate : LocalInstanceTask() {

    @TaskAction
    fun create() {
        localInstanceManager.examineJavaAvailable()

        val createdInstances = localInstanceManager.create(instances.get())
        if (createdInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) created", "Which: ${createdInstances.names}")
        }
    }

    init {
        description = "Creates local Sling instance(s)."
    }

    companion object {
        const val NAME = "instanceCreate"
    }
}
