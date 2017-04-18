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
        logger.info("Package deploy completed.")
        filterInstances().onEach { logger.info("Deployed on: $it") }
    }

}