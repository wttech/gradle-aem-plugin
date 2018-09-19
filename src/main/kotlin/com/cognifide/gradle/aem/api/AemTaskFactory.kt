package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.*
import org.gradle.api.Project
import org.gradle.api.Task

class AemTaskFactory(@Transient private val project: Project) {

    fun get(task: Any): Task {
        return when (task) {
            is String -> project.tasks.getByPath(task)
            is Task -> task
            else -> throw AemException("Illegal task argument: $task")
        }
    }

    fun sequence(tasks: Collection<Any>, afterTasks: Collection<Any>): Collection<Task> {
        val taskList = tasks.map { get(it) }
        val afterList = afterTasks.map { get(it) }

        if (taskList.size > 1) {
            for (i in 1 until taskList.size) {
                val previous = taskList[i - 1]
                val current = taskList[i]

                current.mustRunAfter(previous)
            }
        }
        taskList.forEach { it.mustRunAfter(afterList) }

        return taskList
    }

    fun sequence(name: String, tasks: Collection<Any>, afterTasks: Collection<Any>): Task {
        return project.tasks.create(name) { it.dependsOn(sequence(tasks, afterTasks)); }
    }

    fun setupSequence(name: String, tasks: Collection<Any>): Task {
        val setup = project.tasks.getByName(SetupTask.NAME)
        val create = project.tasks.getByName(CreateTask.NAME)
        val up = project.tasks.getByName(UpTask.NAME)
        val satisfy = project.tasks.getByName(SatisfyTask.NAME)

        val sequence = sequence(name, tasks, listOf(create, up, satisfy))

        setup.dependsOn(sequence)
        sequence.mustRunAfter(create, up, satisfy)

        return sequence
    }

    fun await(): AwaitTask {
        return await(project.tasks.filter { it is AwaitTask }.count().toString())
    }

    fun await(suffix: String): AwaitTask {
        return project.tasks.create("${AwaitTask.NAME}${suffix.capitalize()}", AwaitTask::class.java)
    }


}