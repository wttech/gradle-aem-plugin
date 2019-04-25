package com.cognifide.gradle.aem.environment.tasks

import org.gradle.api.tasks.TaskAction

open class EnvDown : EnvTask() {

    init {
        description = "Turn off local development environment " +
                "- based on configured docker stack name."
    }

    @TaskAction
    fun down() {
        removeStackIfDeployed()
    }

    companion object {
        const val NAME = "aemEnvDown"
    }
}
