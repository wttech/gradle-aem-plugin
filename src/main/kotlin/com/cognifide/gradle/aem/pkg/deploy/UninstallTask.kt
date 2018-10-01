package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class UninstallTask : AemDefaultTask() {

    init {
        description = "Uninstalls AEM package on instance(s)."

        afterConfigured { props.checkForce() }
    }

    @TaskAction
    fun uninstall() {
        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project) { it.uninstallPackage(it.determineRemotePackagePath()) }

        notifier.default("Package uninstalled", "$pkg from ${instances.names}")
    }

    companion object {
        const val NAME = "aemUninstall"
    }

}
