package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.TaskAction

open class ActivateTask : SyncTask() {

    companion object {
        val NAME = "aemActivate"
    }

    init {
        group = AemTask.GROUP
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        val pkg = config.packageFileName
        val instances = Instance.filter(project)

        synchronizeInstances(instances, { it.activatePackage(it.determineRemotePackagePath()) })

        notifier.default("Package activated", "$pkg on ${instances.map { it.name }}")
    }

}
