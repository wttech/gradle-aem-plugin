package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class InstanceDestroy : LocalInstanceTask() {

    init {
        description = "Destroys local AEM instance(s)."
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.props.checkForce()
        }
    }

    @TaskAction
    fun destroy() {
        val createdInstances = instances.filter { it.created }
        if (createdInstances.isEmpty()) {
            logger.info("No instance(s) to destroy")
            return
        }

        logger.info("Destroying instance(s): ${createdInstances.names}")

        aem.progress(createdInstances.size) {
            createdInstances.onEachApply {
                increment("Destroying '$name'") {
                    destroy()
                }
            }
        }

        aem.notifier.notify("Instance(s) destroyed", "Which: ${createdInstances.names}")
    }

    companion object {
        const val NAME = "instanceDestroy"
    }
}