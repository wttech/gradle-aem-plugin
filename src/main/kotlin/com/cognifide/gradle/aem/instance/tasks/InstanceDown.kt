package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceDown : LocalInstanceTask() {

    private var awaitDownOptions: AwaitDownAction.() -> Unit = {}

    fun awaitDown(options: AwaitDownAction.() -> Unit) {
        this.awaitDownOptions = options
    }

    @TaskAction
    fun down() {
        val upInstances = instances.filter { it.running }
        if (upInstances.isEmpty()) {
            logger.lifecycle("No instance(s) to turn off")
            return
        }

        common.progress(upInstances.size) {
            common.parallel.with(upInstances) {
                increment("Stopping instance '$name'") { down() }
            }
        }

        aem.instanceActions.awaitDown {
            instances = upInstances
            awaitDownOptions()
        }

        common.notifier.notify("Instance(s) down", "Which: ${upInstances.names}")
    }

    init {
        description = "Turns off local AEM instance(s)."
    }

    companion object {
        const val NAME = "instanceDown"
    }
}
