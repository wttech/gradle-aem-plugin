package com.cognifide.gradle.aem

import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Internal

interface AemTask : Task {

    @get:Internal
    val aem: AemExtension

    fun projectEvaluated() {
        // intentionally empty
    }

    fun projectsEvaluated() {
        // intentionally empty
    }

    fun taskGraphReady(graph: TaskExecutionGraph) {
        // intentionally empty
    }

    companion object {
        const val GROUP = "AEM"
    }
}
