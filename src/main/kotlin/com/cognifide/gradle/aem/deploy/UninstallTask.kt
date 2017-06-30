package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
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
        synchronize({ sync ->
            propertyParser.checkForce()

            uninstallPackage(determineRemotePackagePath(sync), sync)
        })
    }

}
