package com.cognifide.gradle.aem.environment.tasks

import org.gradle.api.tasks.TaskAction

open class EnvUp : DockerTask() {

    init {
        description = "Starts additional services for local environment " +
            "- based on provided docker compose file."
    }

    @TaskAction
    fun up() = stack.deploy()

    companion object {
        const val NAME = "aemEnvUp"
    }
}
