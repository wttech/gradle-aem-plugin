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
        aem.progress(instances.size) {
            aem.parallel.with(instances) {
                increment("Stopping instance '$name'") { down() }
            }
        }

        aem.instanceActions.awaitDown {
            instances = this@InstanceDown.instances
            awaitDownOptions()
        }

        aem.notifier.notify("Instance(s) down", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "instanceDown"
    }
}