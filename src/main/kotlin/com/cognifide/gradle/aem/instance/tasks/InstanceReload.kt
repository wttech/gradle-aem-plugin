package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.action.ReloadAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.Instance
import org.gradle.api.tasks.TaskAction

open class InstanceReload : Instance() {

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
        instanceManager.awaitReloaded(anyInstances, reloadOptions, awaitUpOptions)
        common.notifier.lifecycle("Instance(s) reloaded", "Which: ${anyInstances.names}")
    }

    init {
        description = "Reloads all AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceReload"
    }
}
