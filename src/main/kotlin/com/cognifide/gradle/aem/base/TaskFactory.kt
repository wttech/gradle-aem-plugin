package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.tasks.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class TaskFactory(@Transient private val project: Project) {

    fun path(path: String): TaskProvider<Task> {
        val projectPath = path.substringBeforeLast(":", project.path)
        val taskName = path.substringAfterLast(":")

        return project.project(projectPath).tasks.named(taskName)
    }

    fun path(paths: Collection<Any>): List<TaskProvider<out Task>> {
        return paths.map {
            when (it) {
                is String -> path(it)
                is TaskProvider<*> -> it
                else -> throw IllegalArgumentException("Illegal task argument: $it")
            }
        }
    }

    fun sequence(name: String, configurer: SequenceOptions.() -> Unit): TaskProvider<Task> {
        return project.tasks.register(name) { sequence ->
            sequence.group = GROUP

            val options = SequenceOptions().apply(configurer)
            val taskList = path(options.dependentTasks)
            val afterList = path(options.afterTasks)

            if (taskList.size > 1) {
                for (i in 1 until taskList.size) {
                    val previous = taskList[i - 1]
                    val current = taskList[i]

                    current.configure { it.mustRunAfter(previous) }
                }
            }

            taskList.forEach { it.configure { task -> task.mustRunAfter(afterList) } }
            sequence.dependsOn(taskList).mustRunAfter(afterList)
        }
    }

    fun setupSequence(name: String, configurer: SequenceOptions.() -> Unit): TaskProvider<Task> {
        val afterTasks = listOf(Create.NAME, Up.NAME, Satisfy.NAME)
        val sequence = sequence(name) { apply(configurer); this.afterTasks = afterTasks }

        project.tasks.named(Setup.NAME).configure { it.dependsOn(sequence) }

        return sequence
    }

    fun await(suffix: String): TaskProvider<Await> {
        val task = project.tasks.register("${Await.NAME}${suffix.capitalize()}", Await::class.java)
        task.configure { it.group = GROUP }

        return task
    }

    class SequenceOptions {
        var dependentTasks: Collection<Any> = listOf()

        var afterTasks: Collection<Any> = listOf()

        fun dependsOn(vararg tasks: Any) {
            dependsOn(tasks.toList())
        }

        fun dependsOn(tasks: Collection<Any>) {
            dependentTasks = tasks
        }

        fun mustRunAfter(vararg tasks: Any) {
            mustRunAfter(tasks.toList())
        }

        fun mustRunAfter(tasks: Collection<Any>) {
            afterTasks = tasks
        }
    }

    companion object {

        const val GROUP = "${AemTask.GROUP} (custom)"
    }
}