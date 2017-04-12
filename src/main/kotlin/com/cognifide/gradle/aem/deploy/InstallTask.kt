package com.cognifide.gradle.aem.deploy

import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory

class InstallTask : DeployTask() {

    companion object {
        val NAME = "aemInstall"

        private val LOG = LoggerFactory.getLogger(InstallTask::class.java)
    }

    @TaskAction
    fun install() {
        deploy { sync ->
            val uploadedPackagePath = determineRemotePackagePath(sync)
            val url = sync.htmlTargetUrl + uploadedPackagePath + "/?cmd=install"

            LOG.info("Installing package using command: " + url)

            val json = sync.post(url, mapOf(
                    "recursive" to config.recursiveInstall,
                    "acHandling" to config.acHandling
            ))
            val response = InstallResponse(json)

            when (response.status) {
                InstallResponse.Status.SUCCESS -> if (response.errors.isEmpty()) {
                    LOG.info("Package successfully installed.")
                } else {
                    LOG.warn("Package installed with errors")
                    response.errors.forEach { LOG.error(it) }
                    throw DeployException("Installation completed with errors!")
                }
                InstallResponse.Status.SUCCESS_WITH_ERRORS -> {
                    LOG.error("Package installed with errors.")
                    response.errors.forEach { LOG.error(it) }
                    throw DeployException("Installation completed with errors!")
                }
                InstallResponse.Status.FAIL -> {
                    LOG.error("Installation failed.")
                    response.errors.forEach { LOG.error(it) }
                    throw DeployException("Installation incomplete!")
                }
            }
        }
    }

}
