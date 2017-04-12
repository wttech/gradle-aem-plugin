package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.apache.commons.io.filefilter.WildcardFileFilter
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.io.File
import java.io.FilenameFilter

abstract class AbstractTask : DefaultTask() {

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
        filterInstances().forEach({ instance -> deployer(DeploySynchronizer(instance, config)) })
    }

    protected fun filterInstances(): List<AemInstance> {
        val props = project.properties
        val instances = if (config.instances.isNotEmpty()) {
            config.instances
        } else {
            instancesFromProps()
        }

        return instances.filter { instance ->
            val type = props.getOrElse("aem.deploy.type", { "*" }) as String

            FilenameUtils.wildcardMatch(instance.type, type, IOCase.INSENSITIVE)
        }
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

    protected fun determineLocalPackage(): File {
        if (!config.localPackagePath.isNullOrBlank()) {
            val configFile = File(config.localPackagePath)
            if (configFile.exists()) {
                return configFile
            }
        }

        val distPath = "${project.buildDir.path}/distributions"
        val typicalPath = "$distPath/${project.name}-${project.version}.zip"
        val typicalFile = File(typicalPath)
        if (typicalFile.exists()) {
            return typicalFile
        }

        val assemblyDir = File("${project.buildDir.path}/distributions")
        val assemblyFilter = WildcardFileFilter(assemblyFilePattern)

        val assemblyFiles = assemblyDir.listFiles(assemblyFilter as FilenameFilter)
        if (assemblyFiles != null && assemblyFiles.isNotEmpty()) {
            return assemblyFiles.first()
        }

        throw DeployException("Local package not found under path: '$typicalPath'. Is it built already?")
    }

    protected fun determineRemotePackagePath(sync: DeploySynchronizer): String {
        if (!config.remotePackagePath.isNullOrBlank()) {
            return config.remotePackagePath
        }

        val url = sync.listPackagesUrl

        logger.info("Asking AEM for uploaded packages: '$url'")

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

        logger.info("Package to be installed found at path: '${path}'")

        return path
    }

}