package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.fileNames
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

open class DeployTask : SyncTask() {

    init {
        description = "Deploys CRX package on instance(s). Upload then install (and optionally activate)."
    }

    /**
     * Enables deployment via CRX package activation from author to publishers when e.g they are not accessible.
     */
    @Input
    var distributed: Boolean = aem.props.flag("aem.deploy.distributed")

    override fun projectsEvaluated() {
        super.projectsEvaluated()

        instances = if (distributed) {
            Instance.filter(project, config.instanceAuthorName)
        } else {
            Instance.filter(project)
        }
    }

    @TaskAction
    fun deploy() {
        aem.syncPackages(instances, packages) { pkg ->
            if (distributed) {
                distributePackage(pkg)
            } else {
                deployPackage(pkg)
            }
        }

        aem.notifier.notify("Package deployed", "${packages.fileNames} on ${instances.names}")
    }

    companion object {
        const val NAME = "aemDeploy"
    }

}