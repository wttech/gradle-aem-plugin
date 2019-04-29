package com.cognifide.gradle.aem.environment.tasks

import org.gradle.api.tasks.TaskAction

open class EnvironmentDown : EnvironmentTask() {

    init {
        description = "Turn off local development environment."
    }

    @TaskAction
    fun down() {
        removeStackIfDeployed()
    }

    companion object {
        const val NAME = "environmentDown"
    }
}
