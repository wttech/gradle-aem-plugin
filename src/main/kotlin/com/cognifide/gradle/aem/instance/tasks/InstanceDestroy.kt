package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.common.utils.onEachApply
import org.gradle.api.tasks.TaskAction

open class InstanceDestroy : LocalInstanceTask() {

    @TaskAction
    fun destroy() {
        val createdInstances = instances.filter { it.touched }
        if (createdInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to destroy")
            return
        }

        logger.info("Destroying instance(s): ${createdInstances.names}")

        common.progress(createdInstances.size) {
            createdInstances.onEachApply {
                increment("Destroying '$name'") {
                    destroy()
                }
            }
        }

        common.notifier.notify("Instance(s) destroyed", "Which: ${createdInstances.names}")
    }

    init {
        description = "Destroys local AEM instance(s)."
        checkForce()
    }

    companion object {
        const val NAME = "instanceDestroy"
    }
}
