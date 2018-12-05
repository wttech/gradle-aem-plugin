package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.instance.InstanceTask
import com.cognifide.gradle.aem.instance.action.ReloadAction
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.TaskAction

open class Reload : InstanceTask() {

    init {
        description = "Reloads all AEM instance(s)."
    }

    private var options: ReloadAction.() -> Unit = {}

    fun options(options: ReloadAction.() -> Unit) {
        this.options = options
    }

    @TaskAction
    fun reload() {
        aem.actions.reload {
            instances = this@Reload.instances
            options()
        }
        aem.notifier.notify("Instance(s) reloaded", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "aemReload"
    }
}