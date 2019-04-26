package com.cognifide.gradle.aem.environment

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.common.file.resolver.FileResolver
import com.cognifide.gradle.aem.environment.checks.HealthChecks
import com.cognifide.gradle.aem.environment.hosts.HostsOptions
import java.io.File
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.util.GFileUtils

class EnvironmentOptions(private val aem: AemExtension) {

    private val fileResolver = FileResolver(aem, AemTask.temporaryDir(aem.project, "environment"))

    @Input
    val directories: MutableList<String> = mutableListOf()

    /**
     * Path in which local AEM environment will be stored.
     */
    var root: String = aem.props.string("aem.env.root") ?: "${aem.projectMain.file(".aem/environment")}"

    /**
     * URI pointing to Dispatcher distribution TAR file.
     */
    @Input
    var dispatcherDistUrl = aem.props.string("aem.env.dispatcher.distUrl")
            ?: "http://download.macromedia.com/dispatcher/download/dispatcher-apache2.4-linux-x86_64-4.3.2.tar.gz"

    @Input
    var dispatcherModuleName = aem.props.string("aem.env.dispatcher.moduleName")
            ?: "*/dispatcher-apache*.so"

    @Internal
    var healthChecks = HealthChecks()

    @Internal
    val hosts = HostsOptions()

    fun healthChecks(options: HealthChecks.() -> Unit) {
        healthChecks = HealthChecks().apply(options)
    }

    fun hosts(config: Map<String, String>) {
        hosts.configure(config)
    }

    /**
     * Ensures that specified directories will exist.
     */
    fun directories(paths: Iterable<String>) {
        directories += paths
    }

    /**
     * Ensures that specified directories will exist.
     */
    fun directories(vararg paths: String) = directories(paths.toList())

    val dockerComposeFile
        get() = File("$root/docker-compose.yml")

    val dockerComposeSourceFile: File
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/docker-compose.yml")

    val httpdConfDir
        get() = File(aem.configCommonDir, "$ENVIRONMENT_DIR/httpd/conf")

    val dispatcherModuleFile: File
        get() = File("$root/$FILES_DIR/mod_dispatcher.so")

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
