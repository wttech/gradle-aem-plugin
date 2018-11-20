package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.base.BaseExtension
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.Nested

abstract class AemDefaultTask : DefaultTask(), AemTask {

    @Nested
    final override val aem = BaseExtension.of(project)

    init {
        group = AemTask.GROUP
    }

    fun willBeExecuted(taskName: String): Boolean {
        return project.gradle.taskGraph.allTasks.any { it.name == taskName }
    }

    fun afterConfigured(callback: Task.() -> Unit) {
        afterConfigured(this, callback)
    }

    fun afterConfigured(task: Task, callback: Task.() -> Unit) {
        project.gradle.taskGraph.whenReady {
            if (it.hasTask(task)) {
                task.apply(callback)
            }
        }
    }

}