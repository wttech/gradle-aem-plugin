package com.cognifide.gradle.aem.environment.tasks

import com.cognifide.gradle.aem.common.Retry
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.checks.ServiceChecker
import com.cognifide.gradle.aem.environment.docker.DockerTask
import com.cognifide.gradle.aem.environment.docker.Stack
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class EnvDown : DockerTask() {

    init {
        description = "Turn off local development environment " +
                "- based on configured docker stack name."
    }

    @Internal
    private val serviceAwait = ServiceChecker(aem)

    @TaskAction
    fun down() {
        stack.rm()
        val isStopped = serviceAwait.awaitConditionObservingProgress("docker network - awaiting stop", NETWORK_STOP_AWAIT_TIME) { stack.isDown() }
        if (!isStopped) {
            throw EnvironmentException("Failed to stop docker stack after ${NETWORK_STOP_AWAIT_TIME / Retry.SECOND_MILIS} seconds." +
                    "\nPlease try to stop it manually by running: `docker stack rm ${Stack.STACK_NAME_DEFAULT}`")
        }
    }

    companion object {
        const val NAME = "aemEnvDown"
        const val NETWORK_STOP_AWAIT_TIME = 30000L
    }
}
