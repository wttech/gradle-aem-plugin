package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.action.AwaitAction
import org.gradle.api.tasks.TaskAction

open class UpTask : AemDefaultTask() {

    companion object {
        val NAME = "aemUp"
    }

    init {
        description = "Turns on local AEM instance(s)."
    }

    @TaskAction
    fun up() {
        val handles = Instance.handles(project)

        handles.parallelStream().forEach { it.up() }
        AwaitAction(project, handles.map { it.instance }).perform()
        handles.parallelStream().forEach { it.init() }

        notifier.default("Instance(s) up and ready", "Which: ${handles.names}")
    }

}