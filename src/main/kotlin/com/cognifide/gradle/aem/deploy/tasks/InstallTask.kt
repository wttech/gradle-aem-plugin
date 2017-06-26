package com.cognifide.gradle.aem.deploy.tasks

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class InstallTask : AbstractTask() {

    companion object {
        val NAME = "aemInstall"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Installs AEM package on instance(s)."
    }

    @TaskAction
    fun install() {
        deploy({ sync ->
            installPackage(determineRemotePackagePath(sync), sync)
        })
    }

}
