package com.cognifide.gradle.aem.environment.docker

import com.cognifide.gradle.aem.environment.Environment
import com.cognifide.gradle.aem.environment.EnvironmentException
import org.gradle.util.GFileUtils
import java.io.File

class Docker(val environment: Environment) {

    val aem = environment.aem

    val running: Boolean
        get() = stack.running && containers.running

    /**
     * Represents Docker stack named 'aem' and provides API for manipulating it.
     */
    val stack = Stack(environment)

    /**
     * Provides API for manipulating Docker containers defined in 'docker-compose.yml'.
     */
    val containers = Containers(this)

    /**
     * Configure additional behavior for Docker containers defined in 'docker-compose.yml'.
     */
    fun <T> containers(options: Containers.() -> T) = containers.run(options)

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
        initAction(this)
    }

    var initAction: Docker.() -> Unit = {}

    fun init(action: Docker.() -> Unit) {
        this.initAction = action
    }

    private fun syncComposeFile() {
        aem.logger.info("Generating Docker compose file '$composeFile' from template '$composeTemplateFile'")

        if (!composeTemplateFile.exists()) {
            throw EnvironmentException("Docker compose file template does not exist: $composeTemplateFile")
        }

        GFileUtils.deleteFileQuietly(composeFile)
        GFileUtils.copyFile(composeTemplateFile, composeFile)
        aem.props.expand(composeFile, mapOf("docker" to this))
    }

    fun up() {
        stack.reset()
        containers.deploy()
    }

    fun reload() {
        containers.reload()
    }

    fun down() {
        stack.undeploy()
    }

    fun ensureDir(vararg paths: String) = paths.forEach { path ->
        environment.file(path).apply { GFileUtils.mkdirs(this) }
    }

    fun cleanDir(vararg paths: String) = paths.forEach { path ->
        environment.file(path).apply { deleteRecursively(); GFileUtils.mkdirs(this) }
    }
}
