package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.environment.docker.Docker
import com.cognifide.gradle.aem.environment.health.HealthChecker
import com.cognifide.gradle.aem.environment.health.HealthStatus
import com.cognifide.gradle.aem.environment.hosts.HostOptions
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable

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

    @JsonIgnore
    val docker = Docker(this)

    fun docker(options: Docker.() -> Unit) {
        docker.apply(options)
    }

    @JsonIgnore
    var healthChecker = HealthChecker(this)

    val hosts = HostOptions(this)

    val created: Boolean
        get() = rootDir.exists()

    @get:JsonIgnore
    val running: Boolean
        get() = docker.running

    fun resolve(): List<File> {
        return docker.containers.defined.flatMap { it.host.resolveFiles() }
    }

    fun up() {
        if (running) {
            aem.logger.info("Environment is already running!")
            return
        }

        aem.logger.info("Turning on: $this")

        docker.init()
        docker.up()

        aem.logger.info("Turned on: $this")
    }

    fun down() {
        if (!running) {
            aem.logger.info("Environment is not yet running!")
            return
        }

        aem.logger.info("Turning off: $this")
        docker.down()
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

    fun check(verbose: Boolean = true): List<HealthStatus> {
        if (!running) {
            throw EnvironmentException("Cannot check environment as it is not running!")
        }

        aem.logger.info("Checking $this")

        return healthChecker.check(verbose)
    }

    fun reload() {
        if (!running) {
            throw EnvironmentException("Cannot reload environment as it is not running!")
        }

        aem.logger.info("Reloading $this")

        docker.reload()

        aem.logger.info("Reloaded $this")
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
    fun hosts(names: Iterable<String>) = hosts.other(names.map { url ->
        if (!url.contains("://")) "http://$url" else url // backward compatibility
    })

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
