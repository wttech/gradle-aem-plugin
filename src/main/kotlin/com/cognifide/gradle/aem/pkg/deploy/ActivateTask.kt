package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class ActivateTask : AemDefaultTask() {

    companion object {
        val NAME = "aemActivate"
    }

    init {
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        val pkg = config.packageFileName
        val instances = Instance.filter(project)

        instances.sync(project) { it.activatePackage(it.determineRemotePackagePath()) }

        notifier.default("Package activated", "$pkg on ${instances.names}")
    }

}
