package com.cognifide.gradle.aem.deploy

import com.cognifide.gradle.aem.AemTask
import org.gradle.api.tasks.TaskAction

open class ActivateTask : SyncTask() {

    companion object {
        val NAME = "aemActivate"
    }

    init {
        group = AemTask.GROUP
        description = "Activates CRX package on instance(s)."
    }

    @TaskAction
    fun activate() {
        synchronize({ sync ->
            activatePackage(determineRemotePackagePath(sync), sync)
        })
    }

}
