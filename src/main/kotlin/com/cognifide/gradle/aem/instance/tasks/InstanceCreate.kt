package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceCreate : LocalInstanceTask() {

    @TaskAction
    fun create() {
        val uncreatedInstances = instances.filter { !it.created }
        if (uncreatedInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to create")
            return
        }

        logger.info("Creating instances: ${uncreatedInstances.names}")

        manager.create(uncreatedInstances)
        val createdInstances = uncreatedInstances.filter { it.created }

        common.notifier.lifecycle("Instance(s) created", "Which: ${createdInstances.names}")
    }

    init {
        description = "Creates local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceCreate"
    }
}
