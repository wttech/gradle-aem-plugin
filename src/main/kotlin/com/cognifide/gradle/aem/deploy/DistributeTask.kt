package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class DistributeTask : AbstractTask() {

    companion object {
        val NAME = "aemDistribute"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Distributes AEM package to instance(s). Upload, install then activate."
    }

    @TaskAction
    fun distribute() {
        logger.info("Package distribution completed.")
        filterInstances().onEach { logger.info("Instance: $it") }
    }

}