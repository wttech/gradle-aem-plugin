package com.cognifide.gradle.aem.instance.tasks

import org.gradle.api.tasks.TaskAction

open class DockerRm : Docker() {

    init {
        description = "Teardown docker compose - local environment."
    }

    @TaskAction
    fun rm() = stack.rm()

    companion object {
        const val NAME = "dockerRm"
    }
}
