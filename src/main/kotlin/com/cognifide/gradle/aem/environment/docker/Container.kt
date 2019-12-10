package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.utils.Formats
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

    val up: Boolean
        get() = running && isLocked(LOCK_UP)

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

                        throw ContainerException(joinToString("\n"))
                    }
                }

                !running
            }
        }
    }

    fun up() {
        await()
        upAction()
        lock(LOCK_UP)
    }

    fun reload() {
        reloadAction()
    }

    fun exec(execSpec: ExecSpec.() -> Unit): DockerResult {
        val spec = ExecSpec(aem).apply(execSpec)
        val operation = spec.operation()

        lateinit var result: DockerResult
        val action = {
            try {
                result = exec(spec)
            } catch (e: DockerException) {
                logger.debug("Exec operation \"$operation\" error", e)
                throw ContainerException("Failed to perform operation \"$operation\" on container '$name'!\n${e.message}")
            }
        }

        if (spec.indicator) {
            aem.progress {
                step = "Container '$name'"
                message = operation

                action()
            }
        } else {
            action()
        }

        return result
    }

    fun exec(command: String, exitCode: Int? = 0) = exec {
        this.command = command
        this.exitCodes = exitCode?.run { listOf(this) } ?: listOf()
    }

    fun exec(operation: String, command: String, exitCode: Int? = 0) = exec {
        this.operation = { operation }
        this.command = command
        this.exitCodes = exitCode?.run { listOf(this) } ?: listOf()
    }

    fun execShell(command: String, exitCode: Int? = 0) = exec("sh -c '$command'", exitCode)

    fun execShell(operation: String, command: String, exitCode: Int? = 0) = exec(operation, "sh -c '$command'", exitCode)

    fun execShellQuiet(command: String, exitCode: Int? = 0) = exec {
        this.indicator = false
        this.command = "sh -c '$command'"
        this.exitCodes = exitCode?.run { listOf(this) } ?: listOf()
    }

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        execShell("Ensuring directory at path '$path'", "mkdir -p $path")
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        execShell("Cleaning directory contents at path '$path'", "rm -fr $path/*")
    }

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

        return DockerProcess.execSpec(customSpec)
    }

    fun lock(name: String) {
        execShellQuiet("mkdir -p $LOCK_ROOT && touch $LOCK_ROOT/$name")
    }

    fun isLocked(name: String): Boolean = execShellQuiet("test -f $LOCK_ROOT/$name", null).exitCode == 0

    companion object {
        const val LOCK_ROOT = "/var/gap/lock"

        const val LOCK_UP = "up"
    }
}

val Collection<Container>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
