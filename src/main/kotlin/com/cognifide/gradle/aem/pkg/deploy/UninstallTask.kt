package com.cognifide.gradle.aem.pkg.deploy

import com.cognifide.gradle.aem.base.api.AemTask
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

        synchronizeInstances({ it.uninstallPackage() })
    }

}
