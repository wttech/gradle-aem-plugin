package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.internal.Behaviors
import org.gradle.api.Project
import com.cognifide.gradle.aem.internal.ProgressLogger as BaseLogger

class ProgressLogger(project: Project, header: String, private val stableTimes: Long) : BaseLogger(project, header) {

    fun progress(states: List<InstanceState>, unavailableInstances: List<Instance>, unstableInstances: List<Instance>, timer: Behaviors.Timer) {
        progress(progressFor(states, unavailableInstances, unstableInstances, timer))
    }

    private fun progressFor(states: List<InstanceState>, unavailableInstances: List<Instance>, unstableInstances: List<Instance>, timer: Behaviors.Timer): String {
        return (progressTicks(timer.ticks, stableTimes) + " " + states.joinToString(" | ") {
            progressFor(it, states.size > 2, unavailableInstances, unstableInstances)
        }).trim()
    }

    private fun progressFor(state: InstanceState, shortProgress: Boolean, unavailableInstances: List<Instance>, unstableInstances: List<Instance>): String {
        return "${state.instance.name} ${progressIndicator(state, unavailableInstances, unstableInstances)}|${progressState(state, shortProgress)}"
    }

    private fun progressTicks(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > PROGRESS_COUNTING_RATIO)) {
            "$tick/$maxTicks"
        } else if (tick.rem(2) == 0L) {
            "/"
        } else {
            "\\"
        }
    }

    private fun progressIndicator(state: InstanceState, unavailableInstances: List<Instance>, unstableInstances: List<Instance>): String {
        return when {
            unavailableInstances.contains(state.instance) -> "-"
            unstableInstances.contains(state.instance) -> "~"
            else -> "+"
        }
    }

    private fun progressState(state: InstanceState, shortInfo: Boolean): String {
        return if (shortInfo) {
            state.bundleState.stablePercent
        } else {
            "${state.bundleState.stablePercent}|${state.bundleState.statsWithLabels}"
        }
    }

    companion object {
        const val PROGRESS_COUNTING_RATIO: Double = 0.1
    }
}