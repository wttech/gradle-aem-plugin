package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemPlugin
import org.gradle.api.tasks.TaskAction

open class ActivateTask : AbstractTask() {

    companion object {
        val NAME = "aemActivate"
    }

    init {
        group = AemPlugin.TASK_GROUP
        description = "Activates AEM package on instance(s)."
    }

    @TaskAction
    fun activate() {
        deploy({ sync ->
            activatePackage(determineRemotePackagePath(sync), sync)
        })
    }

}
