package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.docker.Stack
import org.gradle.api.tasks.TaskAction

open class DockerRm : AemDefaultTask() {

    private val docker = Stack(aem)

    init {
        description = "Teardown docker compose - local environment."
        docker.initSwarm()
    }

    @TaskAction
    fun rm() = docker.rm()

    companion object {
        const val NAME = "dockerRm"
    }
}
