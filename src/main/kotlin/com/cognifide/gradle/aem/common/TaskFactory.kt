package com.cognifide.gradle.aem.common

import com.cognifide.gradle.aem.instance.tasks.Await
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class TaskFactory(@Transient private val project: Project) {

    fun pathed(path: String): TaskProvider<Task> {
        val projectPath = path.substringBeforeLast(":", project.path).ifEmpty { ":" }
        val taskName = path.substringAfterLast(":")

        return project.project(projectPath).tasks.named(taskName)
    }

    fun pathed(paths: Collection<Any>): List<TaskProvider<out Task>> {
        return paths.map { path ->
            when (path) {
                is String -> pathed(path)
                is TaskProvider<*> -> path
                else -> throw IllegalArgumentException("Illegal task argument: $path")
            }
        }
    }

    fun named(name: String) = project.tasks.named(name)

    fun <T : Task> copy(name: String, suffix: String, type: Class<T>, configurer: T.() -> Unit = {}): TaskProvider<T> {
        return project.tasks.register("$name${suffix.capitalize()}", type) { task ->
            task.group = GROUP
            task.apply(configurer)
        }
    }

    fun await(suffix: String, configurer: Await.() -> Unit = {}) = copy(Await.NAME, suffix, Await::class.java, configurer)

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

    fun sequence(name: String, configurer: SequenceOptions.() -> Unit): TaskProvider<Task> {
        val sequence = project.tasks.register(name)

        project.gradle.projectsEvaluated { _ ->
            val options = SequenceOptions().apply(configurer)
            val taskList = pathed(options.dependentTasks)
            val afterList = pathed(options.afterTasks)

            if (taskList.size > 1) {
                for (i in 1 until taskList.size) {
                    val previous = taskList[i - 1]
                    val current = taskList[i]

                    current.configure { it.mustRunAfter(previous) }
                }
            }
            taskList.forEach { task ->
                task.configure { it.mustRunAfter(afterList) }
            }

            sequence.configure { task ->
                task.group = GROUP
                task.dependsOn(taskList).mustRunAfter(afterList)
            }
        }

        return sequence
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