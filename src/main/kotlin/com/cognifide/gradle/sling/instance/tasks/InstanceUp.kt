package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.action.AwaitUpAction
import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.LocalInstanceTask
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
        localInstanceManager.base.examinePrerequisites(instances.get())

        val upInstances = localInstanceManager.up(instances.get(), awaitOptions)
        if (upInstances.isNotEmpty()) {
            common.notifier.lifecycle("Instance(s) up", "Which: ${upInstances.names}")
        }
    }

    init {
        description = "Turns on local Sling instance(s)."
    }

    companion object {
        const val NAME = "instanceUp"
    }
}
