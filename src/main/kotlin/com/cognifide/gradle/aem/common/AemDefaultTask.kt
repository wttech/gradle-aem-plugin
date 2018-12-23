package com.cognifide.gradle.aem.common

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested

open class AemDefaultTask : DefaultTask(), AemTask {

    @Nested
    final override val aem = AemExtension.of(project)

    init {
        group = AemTask.GROUP
    }

    @Internal
    var doProjectEvaluated: () -> Unit = {}

    @Internal
    var doProjectsEvaluated: () -> Unit = {}

    @Internal
    var doTaskGraphReady: (TaskExecutionGraph) -> Unit = {}

    override fun projectEvaluated() {
        doProjectEvaluated()
    }

    override fun projectsEvaluated() {
        doProjectsEvaluated()
    }

    override fun taskGraphReady(graph: TaskExecutionGraph) {
        doTaskGraphReady(graph)
    }

    fun willBeExecuted(taskName: String): Boolean {
        return project.gradle.taskGraph.allTasks.any { it.name == taskName }
    }

    fun afterConfigured(callback: Task.() -> Unit) {
        afterConfigured(this, callback)
    }

    fun afterConfigured(task: Task, callback: Task.() -> Unit) {
        project.gradle.taskGraph.whenReady { graph ->
            if (graph.hasTask(task)) {
                task.apply(callback)
            }
        }
    }
}