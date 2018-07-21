package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.api.AemDefaultTask
import com.cognifide.gradle.aem.instance.action.AwaitAction
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.InstanceStateLogger
import org.apache.commons.httpclient.HttpStatus
import org.gradle.api.tasks.TaskAction

open class DownTask : AemDefaultTask() {

    var stableState = config.awaitStableCheck

    companion object {
        val NAME = "aemDown"
    }

    init {
        description = "Turns off local AEM instance(s)."
    }

    @TaskAction
    fun down() {
        val handles = Instance.handles(project)

        val progressLogger = InstanceStateLogger(project, "Awaiting instance(s) termination")
        progressLogger.started()

        handles.parallelStream().forEach { it.down() }

        Behaviors.waitUntil(config.awaitStableInterval, { timer ->
            val instancesStates = handles.map { InstanceSync(project, it.instance) }.map { it.determineInstanceState() }
            progressLogger.progressState(instancesStates, stableState, config.awaitStableTimes, timer, AwaitAction.PROGRESS_COUNTING_RATIO)

            handles.map { isAemProcessRunning(it) }
                    .reduce { sum, element -> sum && element }
        })

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

    private fun isAemProcessRunning(it: LocalHandle): Boolean = it.pid.exists() && it.controlPort.exists()
}