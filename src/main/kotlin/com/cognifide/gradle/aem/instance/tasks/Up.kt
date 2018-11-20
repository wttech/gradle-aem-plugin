package com.cognifide.gradle.aem.instance.tasks

import com.cognifide.gradle.aem.instance.InstanceTask
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.instance.names
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class Up : InstanceTask() {

    init {
        description = "Turns on local AEM instance(s)."
    }

    @Nested
    val await = AwaitAction(project)

    /**
     * Hook called only when instance is up first time.
     */
    @Internal
    @get:JsonIgnore
    var init: LocalHandle.() -> Unit = { }

    fun await(configurer: AwaitAction.() -> Unit) {
        await.apply(configurer)
    }

    @TaskAction
    fun up() {
        aem.parallelWith(instanceHandles) { up() }
        await.apply { instances = this@Up.instances }.perform()
        aem.parallelWith(instanceHandles) { init(init) }

        aem.notifier.notify("Instance(s) up", "Which: ${instanceHandles.names}")
    }

    companion object {
        const val NAME = "aemUp"
    }

}