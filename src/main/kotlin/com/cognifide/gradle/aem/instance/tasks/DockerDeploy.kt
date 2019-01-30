package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.AemDefaultTask
import com.cognifide.gradle.aem.instance.docker.Stack
import org.gradle.api.tasks.TaskAction

open class DockerDeploy : AemDefaultTask() {

    private val docker = Stack(aem)

    init {
        description = "Setup docker compose - local environment."
        docker.initSwarm()
    }

    @TaskAction
    fun deploy() = docker.deploy()

    companion object {
        const val NAME = "dockerDeploy"
    }
}
