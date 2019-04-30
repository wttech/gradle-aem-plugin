package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import com.cognifide.gradle.aem.environment.service.checker.HealthChecks
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File

class EnvironmentOptions(private val aem: AemExtension) {

    private val distributionsResolver = FileResolver(aem, AemTask.temporaryDir(aem.project, "environment", DISTRIBUTIONS_DIR))

    val directories: MutableList<String> = mutableListOf()

    /**
     * Path in which local AEM environment will be stored.
     */
    var root: String = aem.props.string("env.root") ?: "${aem.projectMain.file(".aem/environment")}"

    @get:JsonIgnore
    val rootDir: File
        get() = File(root)

    @get:JsonIgnore
    val createdLockFile: File
        get() = File(rootDir, "create.lock")

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
    var healthChecks = HealthChecks()

    val hosts = HostsOptions()

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
    fun healthChecks(options: HealthChecks.() -> Unit) {
        healthChecks.apply(options)
    }

    companion object {
        const val ENVIRONMENT_DIR = "environment"

        const val DISTRIBUTIONS_DIR = "distributions"
    }
}
