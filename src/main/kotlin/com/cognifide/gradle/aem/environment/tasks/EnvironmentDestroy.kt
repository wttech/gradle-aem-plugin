package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class EnvironmentDestroy : AemDefaultTask() {

    init {
        description = "Destroys virtualized AEM environment."
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.props.checkForce()
        }
    }

    @TaskAction
    fun destroy() {
        if (!aem.environment.created) {
            aem.notifier.notify("Environment destroyed", "Cannot destroy as it is not created")
            return
        }

        aem.progressIndicator {
            message = "Destroying environment"
            aem.environment.destroy()
        }

        aem.notifier.notify("Environment destroyed")
    }

    companion object {
        const val NAME = "environmentDestroy"
    }
}