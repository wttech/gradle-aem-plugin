package com.cognifide.gradle.aem.instance

import org.gradle.api.tasks.TaskAction

open class DestroyTask : InstanceTask() {

    init {
        description = "Destroys local AEM instance(s)."

        afterConfigured { aem.props.checkForce() }
    }

    @TaskAction
    fun destroy() {
        aem.handles(handles) { destroy() }

        aem.notifier.default("Instance(s) destroyed", "Which: ${handles.names}")
    }

    companion object {
        const val NAME = "aemDestroy"
    }

}