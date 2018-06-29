package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.instance.InstanceState
import org.gradle.api.Project

class InstanceStateLogger(project: Project, header: String) : ProgressLogger(project, header) {

    fun progressState(states: List<InstanceState>, stableCheck: (InstanceState) -> Boolean, stableTimes: Long, timer: Behaviors.Timer, progressCounting: Double){
        progress(progressFor(states, stableCheck, stableTimes,timer,progressCounting))
    }

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

}