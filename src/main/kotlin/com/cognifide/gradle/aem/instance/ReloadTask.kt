package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.ReloadAction
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class ReloadTask : InstanceTask() {

    companion object {
        val NAME = "aemReload"
    }

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
        aem.notifier.default("Instance(s) reloaded", "Which: ${instances.names}")
    }

}