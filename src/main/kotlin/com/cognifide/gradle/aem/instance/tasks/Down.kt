package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.instance.InstanceTask
import com.cognifide.gradle.aem.instance.action.ShutdownAction
import com.cognifide.gradle.aem.instance.names
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Down : InstanceTask() {

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
        shutdown.apply { instances = this@Down.instances }.perform()
        aem.notifier.notify("Instance(s) down", "Which: ${instances.names}")
    }

    companion object {
        const val NAME = "aemDown"
    }

}