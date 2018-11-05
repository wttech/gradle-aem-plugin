package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.ReloadAction
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class ReloadTask : InstanceTask() {

    init {
        description = "Reloads all AEM instance(s)."
    }

    @Nested
    val reload = ReloadAction(project)

    fun reload(configurer: ReloadAction.() -> Unit) {
        reload.apply(configurer)
    }

    @TaskAction
    fun reload() {
        reload.apply { instances = this@ReloadTask.instances }.perform()
        aem.notifier.notify("Instance(s) reloaded", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "aemReload"
    }

}