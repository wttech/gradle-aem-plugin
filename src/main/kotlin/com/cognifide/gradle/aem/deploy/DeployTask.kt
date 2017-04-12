package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FilenameFilter

abstract class DeployTask : DefaultTask() {

    companion object {
        private val LOG = LoggerFactory.getLogger(DeployTask::class.java)
    }

    // TODO this config should be decorated to be able to override it in concrete subproject / task
    /**
     * aemUpload {
     *    aemConfig {
     *       remotePackagePath = 'my-custom-path'
     *    }
     * }
     *
     */
    @Internal
    protected val config = project.extensions.getByType(AemConfig::class.java)

    protected val assemblyFilePattern
        @Input get() = arrayOf(config.assemblyFilePattern, "${project.rootProject.name}-*-${project.version}.zip").filter { !it.isNullOrBlank() }.first()

    protected fun deploy(deployer: (sync: DeploySynchronizer) -> Unit) {
        val props = project.properties
        val instances = if (config.instances.isNotEmpty()) {
            config.instances
        } else {
            instancesFromProps()
        }

        instances.filter { instance ->
            val type = props.getOrElse("aem.deploy.type", { "*" }) as String

            FilenameUtils.wildcardMatch(instance.type, type, IOCase.INSENSITIVE)
        }.forEach({ instance -> deployer(DeploySynchronizer(instance, config)) })
    }

    private fun instancesFromProps(): List<AemInstance> {
        return listOf(
                instanceFromProps("author", 4502),
                instanceFromProps("publish", 4503)
        )
    }

    private fun instanceFromProps(type: String, port: Int): AemInstance {
        val props = project.properties

        return AemInstance(
                props.getOrElse("aem.deploy.$type.url", { "http://localhost:$port" }) as String,
                props.getOrElse("aem.deploy.$type.user", { "admin" }) as String,
                props.getOrElse("aem.deploy.$type.password", { "admin" }) as String,
                type
        )
    }

    protected fun determineLocalPackagePath(): String {
        if (!config.localPackagePath.isNullOrBlank()) {
            return config.localPackagePath
        }

        val typicalPath = "${project.buildDir.path}/distributions/${project.name}-${project.version}.zip"
        val typicalFile = File(typicalPath)
        if (typicalFile.exists()) {
            return typicalFile.absolutePath
        }

        val assemblyDir = File("${project.buildDir.path}/distributions")
        val assemblyFilter = WildcardFileFilter(assemblyFilePattern)

        val assemblyFiles = assemblyDir.listFiles(assemblyFilter as FilenameFilter)
        if (assemblyFiles.isNotEmpty()) {
            return assemblyFiles.first().absolutePath
        }

        return typicalPath
    }

    protected fun determineRemotePackagePath(sync: DeploySynchronizer): String {
        if (!config.remotePackagePath.isNullOrBlank()) {
            return config.remotePackagePath
        }

        val url = sync.listPackagesUrl

        LOG.info("Asking AEM for uploaded packages: '$url'")

        val json = sync.post(sync.listPackagesUrl)

        val response = ListResponse.fromJson(json)
        if (response.results == null) {
            throw DeployException("Cannot ask AEM for uploaded packages!")
        }

        val pid = "${project.group}:${project.name}:${project.version}"
        val result = response.results?.find { result -> result.pid == pid }
        if (result == null) {
            throw DeployException("Package with PID '$pid' is not uploaded on AEM.")
        }

        val path = result.path!!

        LOG.info("Package to be installed found at path: '${path}'")

        return path
    }

}