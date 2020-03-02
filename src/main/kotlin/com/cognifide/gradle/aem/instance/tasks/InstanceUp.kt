package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitUpAction
import com.cognifide.gradle.aem.common.instance.checkRunningOther
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceUp : LocalInstanceTask() {

    private var awaitOptions: AwaitUpAction.() -> Unit = {}

    /**
     * Controls instance awaiting.
     */
    fun await(options: AwaitUpAction.() -> Unit) {
        this.awaitOptions = options
    }

    @TaskAction
    fun up() {
        instances.get().checkRunningOther()

        val upInstances = localInstanceManager.up(instances.get(), awaitOptions)
        if (upInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) up", "Which: ${upInstances.names}")
        }
    }

    init {
        description = "Turns on local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceUp"
    }
}
