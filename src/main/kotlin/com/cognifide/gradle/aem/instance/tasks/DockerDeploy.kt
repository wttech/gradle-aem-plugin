package com.cognifide.gradle.aem.instance.tasks

import org.gradle.api.tasks.TaskAction

open class DockerDeploy : Docker() {

    init {
        description = "Setup docker compose - local environment."
    }

    @TaskAction
    fun deploy() = stack.deploy()

    companion object {
        const val NAME = "dockerDeploy"
    }
}
