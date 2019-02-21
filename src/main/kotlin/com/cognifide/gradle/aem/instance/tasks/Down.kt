package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.common.tasks.LocalInstanceTask
import com.cognifide.gradle.aem.instance.action.ShutdownAction
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.TaskAction

open class Down : LocalInstanceTask() {

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
        aem.actions.shutdown {
            instances = this@Down.instances
            shutdownOptions()
        }

        aem.notifier.notify("Instance(s) down", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "aemDown"
    }
}