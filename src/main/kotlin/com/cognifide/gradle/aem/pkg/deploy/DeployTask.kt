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
        val pkg = config.packageFile
        val instances = if (config.deployDistributed) {
            Instance.filter(project, config.instanceAuthorName)
        } else {
            Instance.filter(project)
        }

        if (config.deployDistributed) {
            synchronizeInstances(instances, { it.distributePackage(pkg) })
        } else {
            synchronizeInstances(instances, { it.deployPackage(pkg) })
        }

        notifier.default("Package deployed", "${pkg.name} on ${instances.joinToString(", ") { it.name }}")
    }

}