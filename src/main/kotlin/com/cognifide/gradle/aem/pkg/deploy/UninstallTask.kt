package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.instance.Instance
import org.gradle.api.tasks.TaskAction

open class UninstallTask : SyncTask() {

    companion object {
        val NAME = "aemUninstall"
    }

    init {
        group = AemTask.GROUP
        description = "Uninstalls AEM package on instance(s)."
    }

    @TaskAction
    fun uninstall() {
        propertyParser.checkForce()

        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        synchronizeInstances(instances, { it.uninstallPackage(it.determineRemotePackagePath()) })

        notifier.default("Package deleted", "$pkg on ${instances.joinToString(", ") { it.name }}")
    }

}
