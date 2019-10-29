package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.container.ContainerException
import com.cognifide.gradle.aem.environment.docker.container.DevOptions
import com.cognifide.gradle.aem.environment.docker.container.HostFileManager
import com.cognifide.gradle.aem.environment.docker.container.ExecSpec
import org.gradle.internal.os.OperatingSystem

class Container(val docker: Docker, val name: String) {

    val aem = docker.aem

    private val logger = aem.logger

    val internalName = "${docker.stack.internalName}_$name"

    val host = HostFileManager(this)

    fun host(options: HostFileManager.() -> Unit) {
        host.apply(options)
    }

    var resolveAction: HostFileManager.() -> Unit = {}

    fun resolve(action: HostFileManager.() -> Unit) {
        resolveAction = action
    }

    fun resolve() {
        resolveAction(host)
    }

    var upAction: Container.() -> Unit = {}

    fun up(action: Container.() -> Unit) {
        upAction = action
    }

    var reloadAction: Container.() -> Unit = {}

    fun reload(action: Container.() -> Unit) {
        reloadAction = action
    }

    val devOptions = DevOptions(this)

    fun dev(options: DevOptions.() -> Unit) {
        devOptions.apply(options)
    }

    var runningTimeout = aem.props.long("environment.docker.container.runningTimeout") ?: 10000L

    val id: String?
        get() {
            try {
                logger.debug("Determining ID for Docker container '$internalName'")

                val containerId = DockerProcess.execString {
                    withArgs("ps", "-l", "-q", "-f", "name=$internalName")
                    withTimeoutMillis(runningTimeout)
                }

                return if (containerId.isBlank()) {
                    null
                } else {
                    containerId
                }
            } catch (e: DockerException) {
                throw ContainerException("Failed to load Docker container ID for name '$internalName'!", e)
            }
        }

    val running: Boolean
        get() {
            val currentId = id ?: return false

            return try {
                logger.debug("Checking running state of Docker container '$name'")

                DockerProcess.execString {
                    withArgs("inspect", "-f", "{{.State.Running}}", currentId)
                    withTimeoutMillis(runningTimeout)
                }.toBoolean()
            } catch (e: DockerException) {
                throw ContainerException("Failed to check Docker container '$name' state!", e)
            }
        }

    var awaitRetry = aem.retry { afterSecond(aem.props.long("environment.docker.container.awaitRetry") ?: 30) }

    fun await() {
        aem.progressIndicator {
            message = "Awaiting container '$name'"
            Behaviors.waitUntil(awaitRetry.delay) { timer ->
                val running = this@Container.running
                if (timer.ticks == awaitRetry.times && !running) {
                    mutableListOf<String>().apply {
                        add("Failed to await container '$name'!")

                        if (OperatingSystem.current().isWindows) {
                            add("Ensure having shared drives configured and reset performed after changing Windows credentials.")
                        }

                        add("Consider troubleshooting:")
                        add("* using command: 'docker stack ps ${docker.stack.internalName} --no-trunc'")
                        add("* restarting Docker")

                        throw EnvironmentException(joinToString("\n"))
                    }
                }

                !running
            }
        }
    }

    fun up() {
        await()

        upAction()
    }

    fun reload() {
        reloadAction()
    }

    fun exec(execSpec: ExecSpec.() -> Unit): DockerResult {
        val spec = ExecSpec().apply(execSpec)
        val operation = spec.operation()

        lateinit var result: DockerResult

        aem.progressIndicator {
            step = "Container '$name'"
            message = operation

            try {
                result = exec(spec)
            } catch (e: DockerException) {
                aem.logger.debug("Exec operation '$operation' error", e)
                throw EnvironmentException("Failed to perform operation '$operation' on container '$name'!\n${e.message}")
            }
        }

        return result
    }

    fun exec(command: String, exitCode: Int = 0) = exec {
        this.command = command
        this.exitCodes = listOf(exitCode)
    }

    fun exec(operation: String, command: String, exitCode: Int = 0) = exec {
        this.operation = { operation }
        this.command = command
        this.exitCodes = listOf(exitCode)
    }

    fun execShell(command: String, exitCode: Int = 0) = exec("sh -c '$command'", exitCode)

    fun execShell(operation: String, command: String, exitCode: Int = 0) = exec(operation, "sh -c '$command'", exitCode)

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        execShell("Ensuring directory at path '$path'", "mkdir -p $path")
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        execShell("Cleaning directory contents at path '$path'", "rm -fr $path/*")
    }

    @Suppress("SpreadOperator")
    private fun exec(spec: ExecSpec): DockerResult {
        if (spec.command.isBlank()) {
            throw ContainerException("Exec command cannot be blank!")
        }

        if (!running) {
            throw ContainerException("Cannot exec command '${spec.command}' since Docker container '$name' is not running!")
        }

        val customSpec = DockerCustomSpec(spec, mutableListOf<String>().apply {
            add("exec")
            addAll(spec.options)
            add(id!!)
            addAll(Formats.commandToArgs(spec.command))
        })

        logger.info("Executing command '${customSpec.fullCommand}' for Docker container '$name'")

        return DockerProcess.execSpec(spec)
    }
}

val Collection<Container>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
