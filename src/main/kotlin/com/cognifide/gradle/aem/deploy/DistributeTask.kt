package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class DistributeTask : AbstractTask() {

    companion object {
        val NAME = "aemDistribute"

        val INSTANCE_FILTER_AUTHOR = "*-author"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Distributes AEM package to instance(s). Upload, install then activate only for instances with group '$INSTANCE_FILTER_AUTHOR'."
    }

    @TaskAction
    fun distribute() {
        deploy({ sync ->
            val packagePath = uploadPackage(determineLocalPackage(), sync).path

            installPackage(packagePath, sync)
            activatePackage(packagePath, sync)
        }, filterInstances(INSTANCE_FILTER_AUTHOR))
    }

}