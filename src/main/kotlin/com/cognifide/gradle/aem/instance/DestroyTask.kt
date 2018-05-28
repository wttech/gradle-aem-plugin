package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class DestroyTask : AemDefaultTask() {

    companion object {
        const val NAME = "aemDestroy"
    }

    init {
        description = "Destroys local AEM instance(s)."
    }

    @TaskAction
    fun destroy() {
        props.checkForce()

        val handles = Instance.handles(project)
        handles.parallelStream().forEach { it.destroy() }

        notifier.default("Instance(s) destroyed", "Which: ${handles.names}")
    }

}