package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import kotlinx.coroutines.*
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.FileOutputStream

class Docker(val environment: Environment) {

    val aem = environment.aem

    private val logger = aem.logger

    val running: Boolean
        get() = stack.running && containers.running

    val up: Boolean
        get() = stack.running && containers.up

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

    val runtime by lazy { Runtime.determine(aem) }

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

        composeFile.takeIf { it.exists() }?.delete()
        composeTemplateFile.copyTo(composeFile)
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

    fun run(spec: RunSpec.() -> Unit) = runInteractive(spec)

    fun run(image: String, command: String, exitCode: Int = 0) = run {
        this.image = image
        this.command = command
        this.exitCodes = listOf(exitCode)
    }

    fun runShell(image: String, command: String, exitCode: Int = 0) = run(image, "sh -c '$command'", exitCode)

    fun run(operation: String, image: String, command: String, exitCode: Int = 0) = run {
        this.operation(operation)
        this.image = image
        this.command = command
        this.exitCode(exitCode)
    }

    fun runShell(operation: String, image: String, command: String, exitCode: Int = 0) = run(operation, image, "sh -c '$command'", exitCode)

    fun runInteractive(spec: RunSpec.() -> Unit) = runInteractive(RunSpec(aem).apply(spec))

    private fun runInteractive(spec: RunSpec): DockerResult {
        val operation = spec.operation

        lateinit var result: DockerResult
        val action = {
            try {
                result = runInternal(spec)
            } catch (e: DockerException) {
                logger.debug("Run operation '$operation' error", e)
                throw EnvironmentException("Failed to run operation on Docker!\n$operation\n${e.message}")
            }
        }

        if (spec.indicator) {
            aem.progress {
                message = operation
                action()
            }
        } else {
            action()
        }

        return result
    }

    private fun runInternal(spec: RunSpec): DockerResult {
        if (spec.image.isBlank()) {
            throw DockerException("Run image cannot be blank!")
        }
        if (spec.command.isBlank()) {
            throw DockerException("Run command cannot be blank!")
        }

        val customSpec = DockerCustomSpec(spec, mutableListOf<String>().apply {
            add("run")
            spec.name?.let { add("--name=$it") }
            if (spec.detached) add("-d")
            addAll(spec.volumes.map { (localPath, containerPath) -> "-v=${runtime.determinePath(localPath)}:$containerPath" })
            addAll(spec.ports.map { (hostPort, containerPort) -> "-p=$hostPort:$containerPort" })
            addAll(spec.options.apply { if (spec.cleanup) add("--rm") else remove("--rm") }.toSet())
            add(spec.image)
            addAll(Formats.commandToArgs(spec.command))
        })

        logger.info("Running Docker command '${customSpec.fullCommand}'")

        return DockerProcess.execSpec(customSpec)
    }

    fun daemon(spec: DaemonSpec.() -> Unit) = daemonInternal(DaemonSpec(aem).apply {
        spec()

        if (image.isBlank()) {
            throw DockerException("Docker daemon specification require image to be defined!")
        }
        if (name == null) {
            name = StringUtils.replace(image, "/", ".")
        }
        outputFile = aem.project.file("build/aem/environment/docker/$name.log")
    })

    private fun daemonInternal(spec: DaemonSpec) = runBlocking {
        when {
            spec.outputFile != null -> logger.lifecycle("Starting Docker daemon \"${spec.operation}\" with logs written to file: ${spec.outputFile}")
            else -> logger.lifecycle("Starting Docker daemon \"${spec.operation}\"")
        }

        val runJob = async(Dispatchers.IO) {
            spec.ignoreExitCodes()
            if (spec.name == null) {
                spec.uniqueName()
            }
            spec.outputFile?.let { file ->
                file.parentFile.mkdirs()

                val fileOut = FileOutputStream(file)
                spec.output = fileOut
                spec.errors = fileOut
            }

            runInteractive(spec)
        }

        delay(spec.initTime)

        while (runJob.isActive) {
            val answer = aem.userInput.askYesNoQuestion("Daemon \"${spec.operation}\" is running. Stop it?")
            if (answer != null && answer == true) {
                break
            }
        }

        logger.info("Stopping Docker daemon \"${spec.operation}\"")

        DockerProcess.execQuietly { withArgs("kill", spec.name) }
        runJob.cancelAndJoin()

        logger.info("Stopped Docker daemon \"${spec.operation}\"")
    }
}
