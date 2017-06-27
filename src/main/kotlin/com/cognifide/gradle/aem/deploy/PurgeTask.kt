package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class PurgeTask : AbstractTask() {

    companion object {
        val NAME = "aemPurge"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Uninstalls and then deletes AEM package on instance(s)."
    }

    @TaskAction
    fun purge() {
        deploy({ sync ->
            propertyParser.checkForce()

            val packagePath = determineRemotePackagePath(sync);

            uninstallPackage(packagePath, sync)
            deletePackage(packagePath, sync)
        })
    }

}
