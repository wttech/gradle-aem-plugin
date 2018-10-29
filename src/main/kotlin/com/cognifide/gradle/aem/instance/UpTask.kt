package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class UpTask : InstanceTask() {

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
    var init: (LocalHandle) -> Unit = { }

    fun await(configurer: AwaitAction.() -> Unit) {
        await.apply(configurer)
    }

    @TaskAction
    fun up() {
        aem.handles(handles) { up() }
        await.apply { instances = this@UpTask.instances }.perform()
        aem.handles(handles) { init(init) }

        aem.notifier.default("Instance(s) up", "Which: ${handles.names}")
    }

    companion object {
        const val NAME = "aemUp"
    }

}