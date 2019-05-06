package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.docker.domain.AemStack
import com.cognifide.gradle.aem.environment.docker.domain.HttpdContainer
import com.cognifide.gradle.aem.environment.health.HealthChecker
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import org.gradle.util.GFileUtils

class Environment(val aem: AemExtension) {

    /**
     * Path in which local AEM environment will be stored.
     */
    var root: String = aem.props.string("env.root") ?: "${aem.projectMain.file(".aem/environment")}"

    @get:JsonIgnore
    val rootDir: File
        get() = File(root)

    /**
     * Represents Docker stack named 'aem' and provides API for manipulating it.
     */
    val stack = AemStack(this)

    /**
     * Represents Docker container named 'aem_httpd' and provides API for manipulating it.
     */
    val httpd = HttpdContainer(this)

    /**
     * Directories to be created if not exist
     */
    val directories: MutableList<String> = mutableListOf()

    /**
     * Allows to provide remote files to Docker containers by mounted volumes.
     */
    val distributionsResolver = FileResolver(aem, AemTask.temporaryDir(aem.project, "environment", DISTRIBUTIONS_DIR))

    /**
     * URI pointing to Dispatcher distribution TAR file.
     */
    var dispatcherDistUrl = aem.props.string("env.dispatcher.distUrl")
            ?: "http://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.2.tar.gz"

    var dispatcherModuleName = aem.props.string("env.dispatcher.moduleName")
            ?: "*/dispatcher-apache*.so"

    @get:JsonIgnore
    val dispatcherModuleSourceFile: File
        get() {
            if (dispatcherDistUrl.isBlank()) {
                throw EnvironmentException("Dispatcher distribution URL needs to be configured in property" +
                        " 'aem.env.dispatcher.distUrl' in order to use AEM environment.")
            }

            val tarFile = distributionsResolver.url(dispatcherDistUrl).file
            val tarTree = aem.project.tarTree(tarFile)

            return tarTree.find { Patterns.wildcard(it, dispatcherModuleName) }
                    ?: throw EnvironmentException("Dispatcher distribution seems to be invalid." +
                            " Cannot find file matching '$dispatcherModuleName' in '$tarFile'")
        }

    val dockerComposeFile
        get() = File(rootDir, "docker-compose.yml")

    val dockerComposeSourceFile: File
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/docker-compose.yml")

    val httpdConfDir
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/httpd/conf")

    val dispatcherModuleFile: File
        get() = File(rootDir, "$DISTRIBUTIONS_DIR/mod_dispatcher.so")

    @JsonIgnore
    var healthChecker = HealthChecker(this)

    val hosts = HostsOptions()

    @get:JsonIgnore
    val createdLockFile: File
        get() = File(rootDir, "create.lock")

    val created: Boolean
        get() = createdLockFile.exists()

    val running: Boolean
        get() = created && stack.running && httpd.running

    fun up() {
        if (running) {
            aem.logger.info("Environment is already running!")
            return
        }

        aem.logger.info("Turning on: $this")

        customize()

        stack.deploy()
        httpd.deploy()

        lock()

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

    private fun lock() {
        FileOperations.lock(createdLockFile)
    }

    private fun provideFiles() {
        if (!dispatcherModuleFile.exists()) {
            GFileUtils.copyFile(dispatcherModuleSourceFile, dispatcherModuleFile)
        }
    }

    private fun syncDockerComposeFile() {
        aem.logger.info("Synchronizing Docker compose file: $dockerComposeSourceFile -> $dockerComposeFile")

        if (!dockerComposeSourceFile.exists()) {
            throw EnvironmentException("Docker compose file does not exist: $dockerComposeSourceFile")
        }

        GFileUtils.deleteFileQuietly(dockerComposeFile)
        GFileUtils.copyFile(dockerComposeSourceFile, dockerComposeFile)
    }

    private fun ensureDirsExist() {
        directories.forEach { dirPath ->
            val dir = File(rootDir, dirPath)
            if (!dir.exists()) {
                aem.logger.info("Creating AEM environment directory: $dir")
                GFileUtils.mkdirs(dir)
            }
        }
    }

    fun check() {
        healthChecker.check()
    }

    /**
     * Ensures that specified directories will exist.
     */
    fun directories(vararg paths: String) = directories(paths.toList())

    /**
     * Ensures that specified directories will exist.
     */
    fun directories(paths: Iterable<String>) {
        directories += paths
    }

    /**
     * Defines hosts to be appended to system specific hosts file.
     */
    fun hosts(vararg values: String) = hosts(values.toList())

    /**
     * Defines hosts to be appended to system specific hosts file.
     */
    fun hosts(values: Iterable<String>) = hosts.define(values)

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