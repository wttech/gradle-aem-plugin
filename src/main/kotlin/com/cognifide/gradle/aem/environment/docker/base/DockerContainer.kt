package com.cognifide.gradle.aem.environment.docker.base

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.docker.DockerException

open class DockerContainer(private val aem: AemExtension, val name: String) {

    private val logger = aem.logger

    var runningTimeout = aem.props.long("environment.dockerContainer.runningTimeout") ?: 10000L

    val id: String?
        get() {
            try {
                logger.debug("Determining ID for Docker container '$name'")

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
            val currentId = id ?: return false

            return try {
                logger.debug("Checking running state of Docker container '$name'")

                Docker.execString {
                    withArgs("inspect", "-f", "{{.State.Running}}", currentId)
                    withTimeoutMillis(runningTimeout)
                }.toBoolean()
            } catch (e: DockerException) {
                throw DockerContainerException("Failed to check Docker container '$name' state!", e)
            }
        }

    @Suppress("SpreadOperator")
    fun exec(spec: DockerExecSpec) {
        if (spec.command.isBlank()) {
            throw DockerContainerException("Exec command cannot be blank!")
        }

        if (!running) {
            throw DockerContainerException("Cannot exec command '${spec.command}' since Docker container '$name' is not running!")
        }

        val args = mutableListOf<String>().apply {
            add("exec")
            addAll(spec.options)
            add(id!!)
            addAll(Formats.commandToArgs(spec.command))
        }
        val fullCommand = args.joinToString(" ")

        logger.info("Executing command '$fullCommand' for Docker container '$name'")

        Docker.exec {
            withArgs(*args.toTypedArray())
            withExpectedExitStatuses(spec.exitCodes.toSet())

            spec.input?.let { withInputStream(it) }
            spec.output?.let { withOutputStream(it) }
            spec.errors?.let { withErrorStream(it) }
        }
    }
}
