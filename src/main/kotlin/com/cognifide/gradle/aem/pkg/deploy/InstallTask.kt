package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.sync
import org.gradle.api.tasks.TaskAction

open class InstallTask : AemDefaultTask() {

    init {
        description = "Installs CRX package on instance(s)."
    }

    @TaskAction
    fun install() {
        val instances = Instance.filter(project)
        val pkg = config.packageFileName

        instances.sync(project) { it.installPackage(it.determineRemotePackagePath()) }

        notifier.default("Package installed", "$pkg on ${instances.names}")
    }

    companion object {
        const val NAME = "aemInstall"
    }

}
