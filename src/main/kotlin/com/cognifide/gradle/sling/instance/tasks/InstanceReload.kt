package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.action.AwaitUpAction
import com.cognifide.gradle.sling.common.instance.action.ReloadAction
import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.InstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceReload : InstanceTask() {

    private var reloadOptions: ReloadAction.() -> Unit = {}

    fun reload(options: ReloadAction.() -> Unit) {
        this.reloadOptions = options
    }

    private var awaitUpOptions: AwaitUpAction.() -> Unit = {}

    fun awaitUp(options: AwaitUpAction.() -> Unit) {
        this.awaitUpOptions = options
    }

    @TaskAction
    fun reload() {
        instanceManager.awaitReloaded(instances.get(), reloadOptions, awaitUpOptions)
        common.notifier.lifecycle("Instance(s) reloaded", "Which: ${instances.get().names}")
    }

    init {
        description = "Reloads all Sling instance(s)."
    }

    companion object {
        const val NAME = "instanceReload"
    }
}
