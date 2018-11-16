package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.instance.InstanceTask
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class Destroy : InstanceTask() {

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
        aem.handles(handles) { destroy() }

        aem.notifier.notify("Instance(s) destroyed", "Which: ${handles.names}")
    }

    companion object {
        const val NAME = "aemDestroy"
    }

}