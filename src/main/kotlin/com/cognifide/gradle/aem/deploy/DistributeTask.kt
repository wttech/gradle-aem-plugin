package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.instance.AemInstance
import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class DistributeTask : SyncTask() {

    companion object {
        val NAME = "aemDistribute"
    }

    init {
        group = AemTask.GROUP
        description = "Distributes CRX package to instance(s). Upload, install then activate only for instances with group '${AemInstance.FILTER_AUTHOR}'."
    }

    @TaskAction
    fun distribute() {
        synchronize({ sync ->
            val packagePath = uploadPackage(determineLocalPackage(), sync).path

            installPackage(packagePath, sync)
            activatePackage(packagePath, sync)
        }, filterInstances(AemInstance.FILTER_AUTHOR))
    }

}