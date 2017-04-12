package com.cognifide.gradle.aem.deploy

import org.gradle.api.tasks.TaskAction

open class DistributeTask : AbstractTask() {

    companion object {
        val NAME = "aemDistribute"
    }

    @TaskAction
    fun distribute() {
        logger.info("Package distribution completed.")
        filterInstances().onEach { logger.info("Instance: $it") }
    }

}