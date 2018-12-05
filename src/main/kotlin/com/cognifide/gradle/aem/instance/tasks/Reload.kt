package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.instance.InstanceTask
import com.cognifide.gradle.aem.instance.action.ReloadAction
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Reload : InstanceTask() {

    init {
        description = "Reloads all AEM instance(s)."
    }

    @Nested
    val reload = ReloadAction(aem)

    fun reload(configurer: ReloadAction.() -> Unit) {
        reload.apply(configurer)
    }

    @TaskAction
    fun reload() {
        reload.apply { instances = this@Reload.instances }.perform()
        aem.notifier.notify("Instance(s) reloaded", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "aemReload"
    }
}