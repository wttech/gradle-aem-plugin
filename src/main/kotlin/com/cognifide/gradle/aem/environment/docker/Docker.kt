package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import com.cognifide.gradle.aem.environment.docker.base.Docker as Base
import org.gradle.util.GFileUtils
import java.io.File

class Docker(val environment: Environment) {

    val aem = environment.aem

    private val logger = aem.logger

    val running: Boolean
        get() = stack.running && containers.running

    /**
     * Represents Docker stack named 'aem' and provides API for manipulating it.
     */
    val stack = Stack(environment)

    /**
     * Provides API for manipulating Docker containers defined in 'docker-compose.yml'.
     */
    val containers = ContainerManager(this)

    /**
     * Configure additional behavior for Docker containers defined in 'docker-compose.yml'.
     */
    fun containers(options: ContainerManager.() -> Unit) {
        containers.apply(options)
    }

    val runtime: Runtime = Runtime.determine(aem)

    val composeFile
        get() = File(environment.rootDir, "docker-compose.yml")

    val composeTemplateFile: File
        get() = File(environment.configDir, "docker-compose.yml.peb")

    val configPath: String
        get() = runtime.determinePath(environment.configDir)

    val rootPath: String
        get() = runtime.determinePath(environment.rootDir)

    fun init() {
        syncComposeFile()
        containers.resolve()
    }

    private fun syncComposeFile() {
        logger.info("Generating Docker compose file '$composeFile' from template '$composeTemplateFile'")

        if (!composeTemplateFile.exists()) {
            throw EnvironmentException("Docker compose file template does not exist: $composeTemplateFile")
        }

        GFileUtils.deleteFileQuietly(composeFile)
        GFileUtils.copyFile(composeTemplateFile, composeFile)
        aem.props.expand(composeFile, mapOf("docker" to this))
    }

    fun up() {
        stack.reset()
        containers.up()
    }

    fun reload() {
        containers.reload()
    }

    fun down() {
        stack.undeploy()
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

    fun exec(options: ExecSpec.() -> Unit) {
        val spec = ExecSpec().apply(options)
        val operation = spec.operation()

        aem.progressIndicator {
            step = "Executing Docker command"
            message = operation

            try {
                exec(spec)
            } catch (e: DockerException) {
                aem.logger.debug("Exec operation '$operation' error", e)
                throw EnvironmentException("Failed to perform operation '$operation' on Docker!\n${e.message}")
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun exec(spec: ExecSpec) {
        if (spec.command.isBlank()) {
            throw DockerException("Exec command cannot be blank!")
        }

        if (!running) {
            throw DockerException("Cannot exec command '${spec.command}'!")
        }

        logger.info("Executing Docker command '${spec.args.joinToString(" ")}'")

        Base.exec {
            withArgs(*spec.args.toTypedArray())
            withExpectedExitStatuses(spec.exitCodes.toSet())

            spec.input?.let { withInputStream(it) }
            spec.output?.let { withOutputStream(it) }
            spec.errors?.let { withErrorStream(it) }
        }
    }
}
