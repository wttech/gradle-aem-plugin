package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.onEachApply
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class Destroy : Instance() {

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
        val handles = localHandles.filter { it.created }
        if (handles.isEmpty()) {
            logger.info("No instance(s) to destroy")
            return
        }

        logger.info("Destroying instance(s): ${handles.names}")

        aem.progress(handles.size) {
            handles.onEachApply {
                increment("Destroying '${instance.name}'") {
                    destroy()
                }
            }
        }

        aem.notifier.notify("Instance(s) destroyed", "Which: ${handles.names}")
    }

    companion object {
        const val NAME = "aemDestroy"
    }
}