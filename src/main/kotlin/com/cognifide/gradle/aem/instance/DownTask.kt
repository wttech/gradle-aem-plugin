package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.ShutdownAction
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class DownTask : InstanceTask() {

    init {
        description = "Turns off local AEM instance(s)."
    }

    @Nested
    val shutdown = ShutdownAction(project)

    fun shutdown(configurer: ShutdownAction.() -> Unit) {
        shutdown.apply(configurer)
    }

    @TaskAction
    fun down() {
        shutdown.apply { instances = this@DownTask.instances }.perform()
        aem.notifier.notify("Instance(s) down", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "aemDown"
    }

}