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

    fun sequence(tasks: Collection<Any>, afterTasks: Collection<Any>): List<TaskProvider<out Task>> {
        val taskList = get(tasks)
        val afterList = get(afterTasks)

        if (taskList.size > 1) {
            for (i in 1 until taskList.size) {
                val previous = taskList[i - 1]
                val current = taskList[i]

                current.configure { it.mustRunAfter(previous) }
            }
        }
        taskList.forEach { it.configure { task -> task.mustRunAfter(afterList) } }

        return taskList
    }

    fun sequence(name: String, tasks: Collection<Any>, afterTasks: Collection<Any>): TaskProvider<Task> {
        return project.tasks.register(name) {
            it.group = "${AemTask.GROUP} (custom)"
            it.dependsOn(sequence(tasks, afterTasks))
        }
    }

    fun setupSequence(name: String, tasks: Collection<Any>): TaskProvider<Task> {
        val afterTasks = listOf(CreateTask.NAME, UpTask.NAME, SatisfyTask.NAME)
        val sequence = sequence(name, tasks, afterTasks)

        project.tasks.named(SetupTask.NAME).configure { it.dependsOn(sequence) }
        sequence.configure { it.mustRunAfter(afterTasks) }

        return sequence
    }

    fun await(suffix: String): TaskProvider<AwaitTask> {
        val task = project.tasks.register("${AwaitTask.NAME}${suffix.capitalize()}", AwaitTask::class.java)
        task.configure { it.group = GROUP }

        return task
    }

    companion object {

        val GROUP = "${AemTask.GROUP} (custom)"

    }

}