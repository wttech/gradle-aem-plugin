package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.common.utils.Formats
import com.cognifide.gradle.aem.environment.docker.base.CygPath
import com.cognifide.gradle.aem.environment.docker.base.DockerRuntime
import com.cognifide.gradle.aem.environment.docker.base.runtime.Toolbox
import com.cognifide.gradle.aem.environment.docker.domain.HttpdContainer
import com.cognifide.gradle.aem.environment.docker.domain.Stack
import com.cognifide.gradle.aem.environment.health.HealthChecker
import com.cognifide.gradle.aem.environment.health.HealthStatus
import com.cognifide.gradle.aem.environment.hosts.HostOptions
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable
import org.gradle.util.GFileUtils

class Environment(@JsonIgnore val aem: AemExtension) : Serializable {

    /**
     * Path in which local AEM environment will be stored.
     */
    var rootDir: File = aem.props.string("environment.rootDir")?.let { aem.project.file(it) }
            ?: aem.projectMain.file(".environment")

    /**
     * Convention directory for storing environment specific configuration files.
     */
    val configDir
        get() = File(aem.configCommonDir, ENVIRONMENT_DIR)

    /**
     * Represents Docker stack named 'aem' and provides API for manipulating it.
     */
    @JsonIgnore
    val stack = Stack(this)

    /**
     * Represents Docker container named 'aem_httpd' and provides API for manipulating it.
     */
    @JsonIgnore
    val httpd = HttpdContainer(this)

    val httpdConfDir
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/httpd/conf")

    /**
     * Directory options (defines caches and regular directories to be created)
     */
    val directories = DirectoryOptions(this)

    private val distributionsResolver = FileResolver(aem, AemTask.temporaryDir(aem.project, "environment", DISTRIBUTIONS_DIR))

    @get:JsonIgnore
    val distributionFiles: List<File>
        get() = distributionsResolver.allFiles

    /**
     * Allows to provide remote files to Docker containers by mounted volumes.
     */
    fun distributions(options: FileResolver.() -> Unit) {
        distributionsResolver.apply(options)
    }

    fun distributionFile(path: String) = File(rootDir, "$DISTRIBUTIONS_DIR/$path")

    val dockerRuntime: DockerRuntime = DockerRuntime.determine(aem)

    val dockerComposeFile
        get() = File(rootDir, "docker-compose.yml")

    val dockerComposeSourceFile: File
        get() = File(configDir, "docker-compose.yml.peb")

    val dockerConfigPath: String
        get() = determineDockerPath(configDir)

    val dockerRootPath: String
        get() = determineDockerPath(rootDir)

    @JsonIgnore
    var healthChecker = HealthChecker(this)

    val hosts = HostOptions(this)

    val created: Boolean
        get() = rootDir.exists()

    @get:JsonIgnore
    val running: Boolean
        get() = stack.running && httpd.running

    fun up() {
        if (running) {
            aem.logger.info("Environment is already running!")
            return
        }

        aem.logger.info("Turning on: $this")

        customize()

        stack.reset()
        httpd.deploy()

        aem.logger.info("Turned on: $this")
    }

    fun down() {
        if (!running) {
            aem.logger.info("Environment is not yet running!")
            return
        }

        aem.logger.info("Turning off: $this")

        stack.undeploy()

        aem.logger.info("Turned off: $this")
    }

    fun restart() {
        down()
        up()
    }

    fun destroy() {
        aem.logger.info("Destroying: $this")

        rootDir.deleteRecursively()

        aem.logger.info("Destroyed: $this")
    }

    private fun customize() {
        aem.logger.info("Customizing AEM environment")

        provideFiles()
        syncDockerComposeFile()
        ensureDirsExist()
    }

    private fun provideFiles() {
        aem.logger.info("Resolving distribution files")
        aem.logger.info("Resolved distribution files:\n${distributionsResolver.allFiles.joinToString("\n")}")
    }

    private fun syncDockerComposeFile() {
        aem.logger.info("Synchronizing Docker compose file: $dockerComposeSourceFile -> $dockerComposeFile")

        if (!dockerComposeSourceFile.exists()) {
            throw EnvironmentException("Docker compose file does not exist: $dockerComposeSourceFile")
        }

        GFileUtils.deleteFileQuietly(dockerComposeFile)
        GFileUtils.copyFile(dockerComposeSourceFile, dockerComposeFile)
        aem.props.expand(dockerComposeFile, mapOf("environment" to this))
    }

    private fun ensureDirsExist() {
        directories.all.forEach { dir ->
            if (!dir.exists()) {
                aem.logger.info("Creating AEM environment directory: $dir")
                GFileUtils.mkdirs(dir)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun determineDockerPath(file: File): String = when (dockerRuntime) {
        is Toolbox -> try {
            CygPath.calculate(file)
        } catch (e: Exception) {
            aem.logger.warn("Cannot determine Docker path for '$file' using 'cygpath', because it is not available.")
            aem.logger.debug("CygPath error", e)
            Formats.normalizePath(file.toString())
        }
        else -> Formats.normalizePath(file.toString())
    }

    fun check(verbose: Boolean = true): List<HealthStatus> {
        if (!running) {
            throw EnvironmentException("Cannot check environment as it is not running!")
        }

        aem.logger.info("Checking $this")

        return healthChecker.check(verbose)
    }

    fun clean() {
        aem.logger.info("Cleaning $this")

        directories.caches.forEach { dir ->
            if (dir.exists()) {
                aem.logger.info("Cleaning AEM environment cache directory: $dir")
                FileOperations.removeDirContents(dir)
            }
        }
    }

    /**
     * Ensures that specified directories will exist.
     */
    fun directories(vararg paths: String) = directories.regular(paths.asIterable())

    /**
     * Allows to distinguish regular directories and caches (cleanable).
     */
    fun directories(options: DirectoryOptions.() -> Unit) {
        directories.apply(options)
    }

    fun hosts(options: HostOptions.() -> Unit) {
        hosts.apply(options)
    }

    /**
     * Defines hosts to be appended to system specific hosts file.
     */
    fun hosts(vararg values: String) = hosts(values.toList())

    /**
     * Defines hosts to be appended to system specific hosts file.
     */
    fun hosts(names: Iterable<String>) = hosts.other(names)

    /**
     * Configures environment service health checks.
     */
    fun healthChecks(options: HealthChecker.() -> Unit) {
        healthChecker.apply(options)
    }

    override fun toString(): String {
        return "Environment(root=$rootDir,running=$running)"
    }

    companion object {
        const val ENVIRONMENT_DIR = "environment"

        const val DISTRIBUTIONS_DIR = "distributions"
    }
}