package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.DockerContainer
import com.cognifide.gradle.aem.environment.docker.container.DevOptions
import com.cognifide.gradle.aem.environment.docker.container.HostFileManager
import com.cognifide.gradle.aem.environment.docker.container.ExecSpec
import org.gradle.internal.os.OperatingSystem

class Container(val docker: Docker, val name: String) {

    val aem = docker.aem

    val base = DockerContainer(aem, "${docker.stack.base.name}_$name")

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

    val running: Boolean
        get() = base.running

    var awaitRetry = aem.retry { afterSecond(aem.props.long("environment.container.awaitRetry") ?: 30) }

    fun await() {
        aem.progressIndicator {
            message = "Awaiting container '$name'"
            Behaviors.waitUntil(awaitRetry.delay) { timer ->
                val running = base.running
                if (timer.ticks == awaitRetry.times && !running) {
                    mutableListOf<String>().apply {
                        add("Failed to await container '$name'!")

                        if (OperatingSystem.current().isWindows) {
                            add("Ensure having shared drives configured and reset performed after changing Windows credentials.")
                        }

                        add("Consider troubleshooting:")
                        add("* using command: 'docker stack ps ${docker.stack.base.name} --no-trunc'")
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

    fun exec(execSpec: ExecSpec.() -> Unit) {
        val spec = ExecSpec().apply(execSpec)
        val operation = spec.operation()

        aem.progressIndicator {
            step = "Container '$name'"
            message = operation

            try {
                base.exec(spec)
            } catch (e: DockerException) {
                throw EnvironmentException("Failed to perform operation '$operation' on container '$name'," +
                        " exit code: '${e.processCause?.exitValue ?: -1 }!", e)
            }
        }
    }

    fun exec(command: String, exitCode: Int = 0) {
        exec { this.command = command; exitCodes = listOf(exitCode) }
    }

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        exec {
            operation { "Ensuring directory at path '$path'" }
            command = "mkdir -p $path"
        }
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        exec {
            operation { "Cleaning directory contents at path '$path'" }
            command = "rm -fr $path/*"
        }
    }
}

val Collection<Container>.names: String
    get() = if (isNotEmpty()) joinToString(", ") { it.name } else "none"
