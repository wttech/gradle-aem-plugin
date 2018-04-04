package com.cognifide.gradle.aem.instance

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

open class PreSatisfyTask : DefaultTask() {

    companion object {
        val NAME = "aemPreSatisfy"
    }

    val satisfyTask = project.tasks.getByName(SatisfyTask.NAME) as SatisfyTask

    @TaskAction
    fun preSatisfy() {
        satisfyTask.packageGroups
    }

}