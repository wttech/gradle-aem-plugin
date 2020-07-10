package com.cognifide.gradle.sling.instance.tasks

import com.cognifide.gradle.sling.common.instance.names
import com.cognifide.gradle.sling.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceDestroy : LocalInstanceTask() {

    @TaskAction
    fun destroy() {
        val destroyedInstances = localInstanceManager.destroy(instances.get())
        if (destroyedInstances.isNotEmpty()) {
            common.notifier.notify("Instance(s) destroyed", "Which: ${destroyedInstances.names}")
        }
    }

    init {
        description = "Destroys local Sling instance(s)."
        checkForce()
    }

    companion object {
        const val NAME = "instanceDestroy"
    }
}
