package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.AemDefaultTask
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.TaskAction

open class EnvironmentDestroy : AemDefaultTask() {

    init {
        description = "Destroys virtualized AEM environment."
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        if (graph.hasTask(this)) {
            aem.prop.checkForce(this)
        }
    }

    @TaskAction
    fun destroy() {
        if (!aem.environment.created) {
            logger.lifecycle("Environment cannot be destroyed as it is not created")
            return
        }

        aem.progressIndicator {
            message = "Destroying environment"
            aem.environment.destroy()
        }

        aem.notifier.lifecycle("Environment destroyed", "Destroyed with success.")
    }

    companion object {
        const val NAME = "environmentDestroy"
    }
}
