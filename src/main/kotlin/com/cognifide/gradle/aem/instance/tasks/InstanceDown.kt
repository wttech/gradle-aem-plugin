package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.AwaitDownAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceDown : LocalInstanceTask() {

    init {
        description = "Turns off local AEM instance(s)."
    }

    private var awaitDownOptions: AwaitDownAction.() -> Unit = {}

    fun awaitDown(options: AwaitDownAction.() -> Unit) {
        this.awaitDownOptions = options
    }

    @TaskAction
    fun down() {
        val upInstances = instances.filter { it.running }
        if (upInstances.isEmpty()) {
            logger.info("No instance(s) to turn off")
            return
        }

        aem.progress(upInstances.size) {
            aem.parallel.with(upInstances) {
                increment("Stopping instance '$name'") { down() }
            }
        }

        aem.instanceActions.awaitDown {
            instances = upInstances
            awaitDownOptions()
        }

        aem.notifier.notify("Instance(s) down", "Which: ${upInstances.names}")
    }

    companion object {
        const val NAME = "instanceDown"
    }
}
