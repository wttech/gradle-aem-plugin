package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class UninstallTask : AbstractTask() {

    companion object {
        val NAME = "aemUninstall"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Uninstalls AEM package on instance(s)."
    }

    @TaskAction
    fun uninstall() {
        deploy({ sync ->
            propertyParser.checkForce()

            uninstallPackage(determineRemotePackagePath(sync), sync)
        })
    }

}
