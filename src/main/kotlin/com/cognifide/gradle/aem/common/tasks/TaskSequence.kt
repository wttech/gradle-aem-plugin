package com.cognifide.gradle.aem.common.tasks

class TaskSequence {

    var dependentTasks: Collection<Any> = listOf()

    var afterTasks: Collection<Any> = listOf()

    fun dependsOrdered(vararg tasks: Any) = dependsOn(tasks)

    fun dependsOn(vararg tasks: Any) {
        dependsOn(tasks.toList())
    }

    fun dependsOn(tasks: Collection<Any>) {
        dependentTasks += tasks
    }

    fun mustRunAfter(vararg tasks: Any) {
        mustRunAfter(tasks.toList())
    }

    fun mustRunAfter(tasks: Collection<Any>) {
        afterTasks += tasks
    }
}
