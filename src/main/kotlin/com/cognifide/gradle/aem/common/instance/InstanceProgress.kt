package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.instance.service.StateChecker

object InstanceProgress {

    const val COUNTING_RATIO: Double = 0.1

    fun determine(
        stableTimes: Long,
        states: List<StateChecker>,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>,
        timer: Behaviors.Timer
    ): String {
        return (ticks(timer.ticks, stableTimes) + " " + states(states, unavailableInstances, unstableInstances)).trim()
    }

    private fun ticks(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > COUNTING_RATIO)) {
            "!${maxTicks - tick}"
        } else if (tick.rem(2) == 0L) {
            "/"
        } else {
            "\\"
        }
    }

    private fun states(states: List<StateChecker>, unavailableInstances: List<Instance>, unstableInstances: List<Instance>): String {
        return states.joinToString(" | ") {
            state(it, states.size > 2, unavailableInstances, unstableInstances)
        }
    }

    private fun state(
        state: StateChecker,
        shortProgress: Boolean,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>
    ): String {
        return "${state.instance.name} ${stateIndicator(state, unavailableInstances, unstableInstances)}|${stateDetails(state, shortProgress)}"
    }

    private fun stateIndicator(
        state: StateChecker,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>
    ): String {
        return when {
            unavailableInstances.contains(state.instance) -> "-"
            unstableInstances.contains(state.instance) -> "~"
            else -> "+"
        }
    }

    private fun stateDetails(state: StateChecker, shortInfo: Boolean): String {
        return if (shortInfo) {
            state.bundleState.stablePercent
        } else {
            "${state.bundleState.stablePercent}|${state.bundleState.statsWithLabels}"
        }
    }
}