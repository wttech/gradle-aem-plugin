package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstance
import org.gradle.api.tasks.TaskAction

open class InstanceDestroy : LocalInstance() {

    @TaskAction
    fun destroy() {
        val destroyedInstances = localInstanceManager.destroy(instances.get())
        if (destroyedInstances.isNotEmpty()) {
            common.notifier.notify("Instance(s) destroyed", "Which: ${destroyedInstances.names}")
        }
    }

    init {
        description = "Destroys local AEM instance(s)."
        checkForce()
    }

    companion object {
        const val NAME = "instanceDestroy"
    }
}
