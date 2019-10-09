package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.DockerContainer
import org.gradle.internal.os.OperatingSystem

class Container(private val docker: Docker, val name: String) {

    private val aem = docker.environment.aem

    val base = DockerContainer(aem, name)

    var reloadAction: Container.() -> Unit = {}

    val running: Boolean
        get() = base.running

    fun deploy() {
        await()
        reload()
    }

    fun reload() {
        reloadAction()
    }

    fun reload(action: Container.() -> Unit) {
        this.reloadAction = action
    }

    var awaitRetry = aem.retry { afterSecond(aem.props.long("environment.container.awaitRetry") ?: 30) }

    fun await() {
        aem.progressIndicator {
            message = "Awaiting container '$name'"
            Behaviors.waitUntil(awaitRetry.delay) { timer ->
                val running = base.running
                if (timer.ticks == awaitRetry.times && !running) {
                    val msg = mutableListOf("Failed to await container '$name'!")
                    if (OperatingSystem.current().isWindows) {
                        msg.add("Ensure having shared drives configured and reset performed after changing Windows credentials.")
                    }
                    msg.add("Consider troubleshooting using command: 'docker stack ps ${docker.stack.base.name} --no-trunc'.")
                    throw EnvironmentException(msg.joinToString("\n"))
                }

                !running
            }
        }
    }

    // DSL

    fun exec(command: String, exitCode: Int? = 0) = exec(command, exitCode?.let { listOf(it) } ?: listOf())

    fun exec(command: String, exitCodes: Iterable<Int>) {
        aem.progressIndicator {
            message = "Executing command '$command' on container '$name'"

            try {
                base.exec(command, exitCodes)
            } catch (e: DockerException) {
                throw EnvironmentException("Failed to execute command '$command' on container '$name', exit code: '${e.processCause?.exitValue ?: -1 }!", e)
            }
        }
    }

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        exec("mkdir -p $path")
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        exec("rm -fr $path")
        exec("mkdir -p $path")
    }
}
