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
        val instances = filterInstances("*-author")

        logger.info("Distributing package to instance(s) (${instances.size})")

        deploy({ sync ->
            val packagePath = uploadPackage(determineLocalPackage(), sync).path

            installPackage(packagePath, sync)
            activatePackage(packagePath, sync)
        }, instances)

        instances.onEach { logger.info("Distributed package on: $it") }
    }

}