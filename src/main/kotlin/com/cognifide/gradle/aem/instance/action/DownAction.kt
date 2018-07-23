package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.Project

class DownAction(project: Project, val instances: List<Instance>) : AbstractAction(project) {

    var stableState = config.awaitStableCheck

    override fun perform() {
        if (instances.isEmpty()) {
            logger.info("No instances to shutdown.")
            return
        }

        shutDownInstances()
    }

    private fun shutDownInstances() {
        val handles = Instance.handles(project)

        val progressLogger = ProgressLogger(project, "Awaiting instance(s) termination: ${instances.names}")
        progressLogger.started()

        handles.parallelStream().forEach { it.down() }

        Behaviors.waitUntil(config.awaitStableInterval) { timer ->
            val instancesStates = handles.map { InstanceSync(project, it.instance) }.map { it.determineInstanceState() }
            progressLogger.progress(progressFor(instancesStates, stableState, config.awaitStableTimes, timer, PROGRESS_COUNTING_RATIO))

            handles.map { isAemProcessRunning(it) }
                    .reduce { sum, element -> sum && element }
        }

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

    private fun isAemProcessRunning(it: LocalHandle): Boolean = it.pidFile.exists() && it.controlPortFile.exists()

    private fun progressFor(states: List<InstanceState>, stableCheck: (InstanceState) -> Boolean, stableTimes: Long, timer: Behaviors.Timer, progressCounting: Double): String {
        return (progressTicks(timer.ticks, stableTimes, progressCounting) + " " + states.joinToString(" | ") { progressFor(it, stableCheck) }).trim()
    }

    private fun progressFor(state: InstanceState, stableCheck: (InstanceState) -> Boolean): String {
        return "${state.instance.name}: ${progressIndicator(stableCheck(state))} ${state.bundleState.statsWithLabels} [${state.bundleState.stablePercent}]"
    }

    private fun progressTicks(tick: Long, maxTicks: Long, progressCounting: Double): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > progressCounting)) {
            "[$tick/$maxTicks]"
        } else if (tick.rem(2) == 0L) {
            "[*]"
        } else {
            "[ ]"
        }
    }

    private fun progressIndicator(state: Boolean): String {
        return if (state) {
            "+"
        } else {
            "-"
        }
    }

    companion object {
        const val PROGRESS_COUNTING_RATIO: Double = 0.1
    }
}