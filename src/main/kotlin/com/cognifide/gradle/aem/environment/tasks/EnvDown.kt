package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.environment.ServiceAwait
import com.cognifide.gradle.aem.environment.docker.DockerTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvDown : DockerTask() {

    init {
        description = "Stops local development environment " +
                "- based on configured docker stack name."
    }

    @Internal
    private val serviceAwait = ServiceAwait(aem)

    private val downDelay = aem.retry { afterSecond(options.downDelay) }

    @TaskAction
    fun down() {
        stack.rm()
        serviceAwait.await("docker network - awaiting stop", downDelay) { stack.isDown() }
    }

    companion object {
        const val NAME = "aemEnvDown"
    }
}
