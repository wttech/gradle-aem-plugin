package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class DeployTask : SyncTask() {

    companion object {
        val NAME = "aemDeploy"
    }

    init {
        group = AemTask.GROUP
        description = "Deploys CRX package on instance(s). Upload then install."
    }

    @TaskAction
    fun deploy() {
        synchronizeInstances({ it.deployPackage() })
    }

}