package com.cognifide.gradle.aem

import org.gradle.api.DefaultTask
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Internal

open class AemDefaultTask : DefaultTask(), AemTask {

    @Internal
    final override val aem = AemExtension.of(project)

    private var doProjectEvaluated: () -> Unit = {}

    private var doProjectsEvaluated: () -> Unit = {}

    private var doTaskGraphReady: (TaskExecutionGraph) -> Unit = {}

    init {
        group = AemTask.GROUP
    }

    override fun projectEvaluated() {
        doProjectEvaluated()
    }

    fun projectEvaluated(callback: () -> Unit) {
        this.doProjectEvaluated = callback
    }

    override fun projectsEvaluated() {
        doProjectsEvaluated()
    }

    fun projectsEvaluated(callback: () -> Unit) {
        this.doProjectsEvaluated = callback
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        doTaskGraphReady(graph)
    }

    fun taskGraphReady(callback: (TaskExecutionGraph) -> Unit) {
        this.doTaskGraphReady = callback
    }

    fun checkForce() {
        taskGraphReady { graph ->
            if (graph.hasTask(this)) {
                aem.prop.checkForce()
            }
        }
    }
}
