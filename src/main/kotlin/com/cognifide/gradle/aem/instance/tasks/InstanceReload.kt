package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceReload : InstanceTask() {

    init {
        description = "Reloads all AEM instance(s)."
    }

    private var reloadOptions: ReloadAction.() -> Unit = {}

    fun reload(options: ReloadAction.() -> Unit) {
        this.reloadOptions = options
    }

    private var awaitOptions: AwaitUpAction.() -> Unit = {}

    fun await(options: AwaitUpAction.() -> Unit) {
        this.awaitOptions = options
    }

    @TaskAction
    fun reload() {
        aem.instanceActions.reload {
            instances = this@InstanceReload.instances
            reloadOptions()
        }
        aem.instanceActions.awaitUp {
            instances = this@InstanceReload.instances
            awaitOptions()
        }

        aem.notifier.notify("Instance(s) reloaded", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "instanceReload"
    }
}