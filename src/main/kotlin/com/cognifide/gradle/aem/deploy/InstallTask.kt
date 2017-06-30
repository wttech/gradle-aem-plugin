package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class InstallTask : SyncTask() {

    companion object {
        val NAME = "aemInstall"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Installs AEM package on instance(s)."
    }

    @TaskAction
    fun install() {
        synchronize({ sync ->
            installPackage(determineRemotePackagePath(sync), sync)
        })
    }

}
