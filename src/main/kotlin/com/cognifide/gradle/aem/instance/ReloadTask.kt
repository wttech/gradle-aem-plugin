package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.action.ReloadAction
import org.gradle.api.tasks.TaskAction

open class ReloadTask : AemDefaultTask() {

    companion object {
        val NAME = "aemReload"
    }

    init {
        description = "Reloads all AEM instance(s)."
    }

    @TaskAction
    fun reload() {
        val instances = Instance.filter(project)

        notifier.default("Instance(s) reloading", "Which: ${instances.names}")
        ReloadAction(project, instances).perform()
        notifier.default("Instance(s) reloaded","Which: ${instances.names}")
    }

}