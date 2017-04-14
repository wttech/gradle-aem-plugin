package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.AemTask
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import java.io.File

abstract class AbstractTask : DefaultTask(), AemTask {

    @Input
    override val config = AemConfig.extendFromGlobal(project)

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

        throw DeployException("Local package not found under path: '$typicalPath'. Is it built already?")
    }

    protected fun determineRemotePackagePath(sync: DeploySynchronizer): String {
        if (!config.remotePackagePath.isNullOrBlank()) {
            return config.remotePackagePath
        }

        val url = sync.listPackagesUrl

        logger.info("Asking AEM for uploaded packages: '$url'")

        val json = sync.post(sync.listPackagesUrl)
        val response = try {
            ListResponse.fromJson(json)
        } catch (e : Exception) {
            throw DeployException("Cannot ask AEM for uploaded packages!")
        }

        val pid = "${project.group}:${project.name}:${project.version}"
        val result = response.results.find { result -> result.pid == pid }
        if (result == null) {
            throw DeployException("Package with PID '$pid' is not uploaded on AEM.")
        }

        val path = result.path!!

        logger.info("Package to be installed found at path: '${path}'")

        return path
    }

}