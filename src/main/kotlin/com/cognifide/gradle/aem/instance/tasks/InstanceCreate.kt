package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.tasks.LocalInstance
import org.gradle.api.tasks.TaskAction

open class InstanceCreate : LocalInstance() {

    @TaskAction
    fun create() {
        localInstanceManager.examineJavaAvailable()

        val createdInstances = localInstanceManager.create(instances.get())
        if (createdInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) created", "Which: ${createdInstances.names}")
        }
    }

    init {
        description = "Creates local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceCreate"
    }
}
