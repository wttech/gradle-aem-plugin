package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class DeployTask : SyncTask() {

    companion object {
        val NAME = "aemDeploy"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Deploys AEM package on instance(s). Upload then install."
    }

    @TaskAction
    fun deploy() {
        synchronize({ sync ->
            val packagePath = uploadPackage(determineLocalPackage(), sync).path

            installPackage(packagePath, sync)
        })
    }

}