package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.TaskAction

open class DeployTask : SyncTask() {

    companion object {
        val NAME = "aemDeploy"
    }

    init {
        group = AemTask.GROUP
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
    }

    @TaskAction
    fun deploy() {
        if (config.deployDistributed) {
            synchronizeInstances({ it.distributePackage() }, Instance.filter(project, config.deployInstanceAuthorName))
        } else {
            synchronizeInstances({ it.deployPackage() })
        }
    }

}