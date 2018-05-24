package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class UninstallTask : AemDefaultTask() {

    companion object {
        val NAME = "aemUninstall"
    }

    init {
        description = "Uninstalls AEM package on instance(s)."
    }

    @TaskAction
    fun uninstall() {
        propertyParser.checkForce()

        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project, { it.uninstallPackage(it.determineRemotePackagePath()) })

        notifier.default("Package uninstalled", "$pkg from ${instances.names}")
    }

}
