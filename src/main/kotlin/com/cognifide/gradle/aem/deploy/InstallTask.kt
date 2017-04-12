package com.cognifide.gradle.aem.deploy

import org.gradle.api.tasks.TaskAction

open class InstallTask : AbstractTask() {

    companion object {
        val NAME = "aemInstall"
    }

    @TaskAction
    fun install() {
        deploy { sync ->
            val uploadedPackagePath = determineRemotePackagePath(sync)
            val url = sync.htmlTargetUrl + uploadedPackagePath + "/?cmd=install"

            logger.info("Installing package using command: " + url)

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
        }
    }

}
