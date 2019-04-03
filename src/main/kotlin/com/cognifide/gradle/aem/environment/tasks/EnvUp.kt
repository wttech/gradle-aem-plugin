package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.environment.ServiceAwait
import com.cognifide.gradle.aem.environment.docker.DockerTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvUp : DockerTask() {

    init {
        description = "Starts additional services for local environment " +
                "- based on provided docker compose file."
    }

    @Internal
    private val serviceAwait = ServiceAwait(aem)

    @TaskAction
    fun up() {
        stack.deploy(config.composeFilePath)
        serviceAwait.await()
    }

    companion object {
        const val NAME = "aemEnvUp"
    }
}
