package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import org.gradle.api.tasks.TaskAction

open class DownTask : AemDefaultTask() {

    companion object {
        val NAME = "aemDown"
    }

    init {
        description = "Turns off local AEM instance(s)."
    }

    @TaskAction
    fun down() {
        val handles = Instance.handles(project)
        handles.parallelStream().forEach { it.down() }

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

}