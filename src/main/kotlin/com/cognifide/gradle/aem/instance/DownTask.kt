package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
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

        val progressLogger = ProgressLogger(project, "Awaiting instance(s) termination")
        progressLogger.started()

        handles.parallelStream().forEach { handle ->
            handle.down()
            Behaviors.waitUntil(config.awaitStableInterval, { timer ->
                return@waitUntil isAemProcessRunning(handle)
            })
        }

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

    private fun isAemProcessRunning(it: LocalHandle): Boolean = it.pid.exists() && it.controlPort.exists()

}