package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceReload : InstanceTask() {

    init {
        description = "Reloads all AEM instance(s)."
    }

    private var options: ReloadAction.() -> Unit = {}

    fun options(options: ReloadAction.() -> Unit) {
        this.options = options
    }

    @TaskAction
    fun reload() {
        aem.instanceActions.reload {
            instances = this@InstanceReload.instances
            options()
        }
        aem.notifier.notify("Instance(s) reloaded", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "instanceReload"
    }
}