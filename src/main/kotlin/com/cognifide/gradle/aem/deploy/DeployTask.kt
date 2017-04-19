package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class DeployTask : AbstractTask() {

    companion object {
        val NAME = "aemDeploy"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Deploys AEM package on instance(s). Upload then install."
    }

    @TaskAction
    fun deploy() {
        val instances = filterInstances()

        logger.info("Deploying package on instance(s) (${instances.size})")

        deploy({ sync ->
            val packagePath = uploadPackage(determineLocalPackage(), sync).path

            installPackage(packagePath, sync)
        })

        instances.onEach { logger.info("Deployed package on: $it") }
    }

}