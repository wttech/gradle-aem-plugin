package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Nested
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

abstract class AbstractTask : DefaultTask(), AemTask {

    @Nested
    final override val config = AemConfig.of(project)

    protected fun deploy(deployer: (sync: DeploySynchronizer) -> Unit, instances: List<AemInstance> = filterInstances()) {
        val callback = { instance: AemInstance -> deploy(deployer, instance) }
        if (config.deployParallel) {
            instances.parallelStream().forEach(callback)
        } else {
            instances.onEach(callback)
        }
    }

    protected fun deploy(deployer: (sync: DeploySynchronizer) -> Unit, instance: AemInstance) {
        logger.info("Deploying on: $instance")

        deployer(DeploySynchronizer(instance, config))
    }

    protected fun filterInstances(instanceGroup: String = AemInstance.FILTER_DEFAULT): List<AemInstance> {
        return AemInstance.filter(project, instanceGroup)
    }

    protected fun determineLocalPackage(): File {
        if (!config.localPackagePath.isNullOrBlank()) {
            val configFile = File(config.localPackagePath)
            if (configFile.exists()) {
                return configFile
            }
        }

        val typicalFile = File("${project.buildDir.path}/distributions/${project.name}-${project.version}.zip")
        if (typicalFile.exists()) {
            return typicalFile
        }

        throw DeployException("Local package not found under path: '${typicalFile.absolutePath}'. Is it built already?")
    }

    protected fun determineRemotePackagePath(sync: DeploySynchronizer): String {
        if (!config.remotePackagePath.isNullOrBlank()) {
            return config.remotePackagePath
        }

        val url = sync.listPackagesUrl

        logger.info("Asking AEM for uploaded packages using URL: '$url'")

        val json = sync.post(sync.listPackagesUrl)
        val response = try {
            ListResponse.fromJson(json)
        } catch (e: Exception) {
            throw DeployException("Cannot ask AEM for uploaded packages!")
        }

        val path = response.resolvePath(project)
        if (path.isNullOrBlank()) {
            throw DeployException("Package is not uploaded on AEM.")
        }

        logger.info("Package found on AEM at path: '$path'")

        return path!!
    }

    protected fun uploadPackage(file: File, sync: DeploySynchronizer): UploadResponse {
        val url = sync.jsonTargetUrl + "/?cmd=upload"

        logger.info("Uploading package at path '{}' to URL '{}'", file.path, url)

        try {
            val json = sync.post(url, mapOf(
                    "package" to file,
                    "force" to config.uploadForce
            ))
            val response = UploadResponse.fromJson(json)

            if (response.isSuccess) {
                logger.info(response.msg)
            } else {
                logger.error(response.msg)
                throw DeployException(response.msg.orEmpty())
            }

            return response
        } catch (e: FileNotFoundException) {
            throw DeployException(String.format("Package file '%s' not found!", file.path), e)
        } catch (e: Exception) {
            throw DeployException("Cannot upload package", e)
        }
    }

    protected fun installPackage(uploadedPackagePath: String, sync: DeploySynchronizer): InstallResponse {
        val url = sync.htmlTargetUrl + uploadedPackagePath + "/?cmd=install"

        logger.info("Installing package using command: " + url)

        try {
            val json = sync.post(url, mapOf(
                    "recursive" to config.recursiveInstall,
                    "acHandling" to config.acHandling
            ))
            val response = InstallResponse(json)

            when (response.status) {
                InstallResponse.Status.SUCCESS -> if (response.errors.isEmpty()) {
                    logger.info("Package successfully installed.")
                } else {
                    logger.warn("Package installed with errors")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Installation completed with errors!")
                }
                InstallResponse.Status.SUCCESS_WITH_ERRORS -> {
                    logger.error("Package installed with errors.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Installation completed with errors!")
                }
                InstallResponse.Status.FAIL -> {
                    logger.error("Installation failed.")
                    response.errors.forEach { logger.error(it) }
                    throw DeployException("Installation incomplete!")
                }
            }

            return response
        } catch (e: Exception) {
            throw DeployException("Cannot install package", e)
        }
    }

    protected fun activatePackage(path: String, sync: DeploySynchronizer): UploadResponse {
        val url = sync.jsonTargetUrl + path + "/?cmd=replicate"

        logger.info("Activating package using command: " + url)

        val json: String
        try {
            json = sync.post(url)
        } catch (e: DeployException) {
            throw DeployException("Cannot activate package", e)
        }

        val response: UploadResponse = try {
            UploadResponse.fromJson(json)
        } catch (e: IOException) {
            logger.error("Malformed JSON response", e)
            throw DeployException("Package activation failed", e)
        }

        if (response.isSuccess) {
            logger.info("Package activated")
        } else {
            logger.error("Package activation failed: + " + response.msg)
            throw DeployException(response.msg.orEmpty())
        }

        return response
    }

}