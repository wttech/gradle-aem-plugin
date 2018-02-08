package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.pkg.deploy.SyncTask
import org.gradle.api.tasks.TaskAction

open class ReloadTask : SyncTask() {

    companion object {
        val NAME = "aemReload"
    }

    init {
        description = "Reloads all AEM instance(s)."
    }

    @TaskAction
    fun reload() {
        synchronizeInstances { it.reload() }
        awaitStableInstances()
    }

}