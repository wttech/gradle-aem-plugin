package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemInstance
import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class DistributeTask : AbstractTask() {

    companion object {
        val NAME = "aemDistribute"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Distributes AEM package to instance(s). Upload, install then activate only for instances with group '${AemInstance.FILTER_AUTHOR}'."
    }

    @TaskAction
    fun distribute() {
        deploy({ sync ->
            val packagePath = uploadPackage(determineLocalPackage(), sync).path

            installPackage(packagePath, sync)
            activatePackage(packagePath, sync)
        }, filterInstances(AemInstance.FILTER_AUTHOR))
    }

}