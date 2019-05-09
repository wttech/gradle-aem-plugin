package com.cognifide.gradle.aem.environment.docker.base

open class DockerContainer(val name: String) {

    var runningTimeout = 10000L

    val id: String?
        get() {
            try {
                val containerId = Docker.execString {
                    withArgs("ps", "-l", "-q", "-f", "name=$name")
                    withTimeoutMillis(runningTimeout)
                }

                return if (containerId.isBlank()) {
                    null
                } else {
                    containerId
                }
            } catch (e: DockerException) {
                throw DockerContainerException("Failed to load Docker container ID for name '$name'!", e)
            }
        }

    val running: Boolean
        get() {
            val containerId = id ?: return false

            try {
                return Docker.execString {
                    withArgs("inspect", "-f", "{{.State.Running}}", containerId)
                    withTimeoutMillis(runningTimeout)
                }.toBoolean()
            } catch (e: DockerException) {
                throw DockerContainerException("Failed to check Docker container '$name' state!", e)
            }
        }

    @Suppress("SpreadOperator")
    fun exec(command: String, exitCode: Int = 0) {
        if (!running) {
            throw DockerContainerException("Cannot exec command '$command' since Docker container '$name' is not running!")
        }

        Docker.exec {
            withArgs("exec", id, *command.split(" ").toTypedArray())
            withExpectedExitStatuses(exitCode)
        }
    }
}