package com.cognifide.gradle.aem.deploy.tasks

import com.cognifide.gradle.aem.AemPlugin

open class PurgeTask : AbstractTask() {

    companion object {
        val NAME = "aemPurge"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Uninstalls and then deletes AEM package on instance(s)."
    }

    @org.gradle.api.tasks.TaskAction
    fun install() {
        deploy({ sync ->
            ensureUserAwareness(NAME)

            val packageToPurge = determineRemotePackagePath(sync);

            uninstallPackage(packageToPurge, sync)
            deletePackage(packageToPurge, sync)
        })
    }

}
