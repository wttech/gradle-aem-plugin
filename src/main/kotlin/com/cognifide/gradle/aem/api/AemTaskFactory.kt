package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.*
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

class AemTaskFactory(@Transient private val project: Project) {

    fun get(tasks: Collection<Any>): List<TaskProvider<out Task>> {
        return tasks.map {
            when (it) {
                is String -> project.tasks.named(it)
                is TaskProvider<*> -> it
                else -> throw IllegalArgumentException("Illegal task argument: $it")
            }
        }
    }

    fun sequence(name: String, configurer: SequenceOptions.() -> Unit): TaskProvider<Task> {
        return project.tasks.register(name) { sequence ->
            sequence.group = GROUP

            val options = SequenceOptions().apply(configurer)
            val taskList = get(options.dependentTasks)
            val afterList = get(options.afterTasks)

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
        val afterTasks = listOf(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME)
        val sequence = sequence(name) { apply(configurer); this.afterTasks = afterTasks }

        project.tasks.named(SetupTask.NAME).configure { it.dependsOn(sequence) }

        return sequence
    }

    fun await(suffix: String): TaskProvider<AwaitTask> {
        val task = project.tasks.register("${AwaitTask.NAME}${suffix.capitalize()}", AwaitTask::class.java)
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

        val GROUP = "${AemTask.GROUP} (custom)"

    }

}