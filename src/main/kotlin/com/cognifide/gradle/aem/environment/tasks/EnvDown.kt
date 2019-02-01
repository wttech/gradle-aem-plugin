package com.cognifide.gradle.aem.environment.tasks

import org.gradle.api.tasks.TaskAction

open class EnvDown : DockerTask() {

    init {
        description = "Stops local development environment " +
            "- based on configured docker stack name."
    }

    @TaskAction
    fun down() = stack.rm()

    companion object {
        const val NAME = "aemEnvDown"
    }
}
