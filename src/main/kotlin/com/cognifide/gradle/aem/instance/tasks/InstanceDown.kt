package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.instance.action.ShutdownAction
import com.cognifide.gradle.aem.common.instance.names
import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import org.gradle.api.tasks.TaskAction

open class InstanceDown : LocalInstanceTask() {

    init {
        description = "Turns off local AEM instance(s)."
    }

    private var shutdownOptions: ShutdownAction.() -> Unit = {}

    /**
     * Controls shutdown action.
     */
    fun shutdown(options: ShutdownAction.() -> Unit) {
        this.shutdownOptions = options
    }

    @TaskAction
    fun down() {
        aem.instanceActions.shutdown {
            instances = this@InstanceDown.instances
            shutdownOptions()
        }

        aem.notifier.notify("Instance(s) down", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "instanceDown"
    }
}