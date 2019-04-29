package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.checks.HealthChecks
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import org.gradle.util.GFileUtils

class EnvironmentOptions(private val aem: AemExtension) {

    private val fileResolver = FileResolver(aem, AemTask.temporaryDir(aem.project, "environment"))

    val directories: MutableList<String> = mutableListOf()

    /**
     * Path in which local AEM environment will be stored.
     */
    var root: String = aem.props.string("aem.env.root") ?: "${aem.projectMain.file(".aem/environment")}"

    /**
     * URI pointing to Dispatcher distribution TAR file.
     */
    var dispatcherDistUrl = aem.props.string("aem.env.dispatcher.distUrl")
            ?: "http://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.2.tar.gz"

    var dispatcherModuleName = aem.props.string("aem.env.dispatcher.moduleName")
            ?: "*/dispatcher-apache*.so"

    val dockerComposeFile
        get() = File("$root/docker-compose.yml")

    val dockerComposeSourceFile: File
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/docker-compose.yml")

    val httpdConfDir
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/httpd/conf")

    val dispatcherModuleFile: File
        get() = File("$root/$FILES_DIR/mod_dispatcher.so")

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

    fun prepare() {
        provideFiles()
        syncDockerComposeFile()
        ensureDirsExist()
    }

    private fun provideFiles() {
        if (!dispatcherModuleFile.exists()) {
            GFileUtils.copyFile(downloadDispatcherModule(), dispatcherModuleFile)
        }
    }

    private fun syncDockerComposeFile() {
        GFileUtils.deleteFileQuietly(dockerComposeFile)
        GFileUtils.copyFile(dockerComposeSourceFile, dockerComposeFile)
    }

    fun validate() {
        if (!dockerComposeFile.exists()) {
            throw EnvironmentException("Docker compose file does not exist: $dockerComposeFile")
        }
    }

    private fun downloadDispatcherModule(): File {
        if (dispatcherDistUrl.isBlank()) {
            throw EnvironmentException("Dispatcher distribution URL needs to be configured in property" +
                    " 'aem.env.dispatcher.distUrl' in order to use AEM environment.")
        }

        val tarFile = fileResolver.url(dispatcherDistUrl).file
        val tarTree = aem.project.tarTree(tarFile)

        return tarTree.find { Patterns.wildcard(it, dispatcherModuleName) }
                ?: throw EnvironmentException("Dispatcher distribution seems to be invalid." +
                        " Cannot find file matching '$dispatcherModuleName' in '$tarFile'")
    }

    private fun ensureDirsExist() {
        directories.forEach { GFileUtils.mkdirs(File("$root/$it")) }
    }

    companion object {
        const val ENVIRONMENT_DIR = "environment"

        const val FILES_DIR = "files"
    }
}
