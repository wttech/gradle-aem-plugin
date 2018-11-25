package com.cognifide.gradle.aem.base

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.tasks.*
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class TaskFactory(@Transient private val project: Project) {

    fun <T : Task> register(name: String, clazz: Class<T>): TaskProvider<T> {
        return register(name, clazz, Action {})
    }

    fun <T : Task> register(name: String, clazz: Class<T>, configurer: (T) -> Unit): TaskProvider<T> {
        return register(name, clazz, Action { configurer(it) })
    }

    fun <T : Task> register(name: String, clazz: Class<T>, configurer: Action<T>): TaskProvider<T> {
        with(project) {
            val provider = tasks.register(name, clazz, configurer)

            afterEvaluate { provider.configure { if (it is AemTask) it.projectEvaluated() } }
            gradle.projectsEvaluated { provider.configure { if (it is AemTask) it.projectsEvaluated() } }
            gradle.taskGraph.whenReady { graph -> provider.configure { if (it is AemTask) it.taskGraphReady(graph) } }

            return provider
        }
    }

    fun path(path: String): TaskProvider<Task> {
        val projectPath = path.substringBeforeLast(":", project.path).ifEmpty { ":" }
        val taskName = path.substringAfterLast(":")

        return project.project(projectPath).tasks.named(taskName)
    }

    fun path(paths: Collection<Any>): List<TaskProvider<out Task>> {
        return paths.map { path ->
            when (path) {
                is String -> path(path)
                is TaskProvider<*> -> path
                else -> throw IllegalArgumentException("Illegal task argument: $path")
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