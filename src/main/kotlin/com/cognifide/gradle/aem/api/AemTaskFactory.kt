package com.cognifide.gradle.aem.api

import com.cognifide.gradle.aem.instance.AwaitTask
import org.gradle.api.Project
import org.gradle.api.Task

class AemTaskFactory(@Transient private val project: Project) {

    fun get(task: Any): Task {
        return when (task) {
            is String -> project.tasks.getByPath(task)
            is Task -> task
            else -> throw AemException("Illegal task argument")
        }
    }

    fun sequence(name: String, tasks: Collection<Any>): Task {
        return project.tasks.create(name) { it.dependsOn(sequence(tasks)) }
    }

    fun sequence(tasks: Collection<Any>): Collection<Task> {
        val list = tasks.map { get(it) }

        if (list.size > 1) {
            for (i in 1 until list.size) {
                val previous = list[i - 1]
                val current = list[i]

                current.mustRunAfter(previous)
            }
        }

        return list
    }

    fun await(): AwaitTask {
        return await(project.tasks.filter { it is AwaitTask }.count().toString())
    }

    fun await(suffix: String): AwaitTask {
        return project.tasks.create("${AwaitTask.NAME}${suffix.capitalize()}", AwaitTask::class.java)
    }


}