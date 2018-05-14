package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.ReloadAction
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
        val instances = Instance.filter(project)
        ReloadAction(project, instances).perform()

        notifier.default("Instance(s) reloaded", instances.joinToString(", ") { it.name })
    }

}