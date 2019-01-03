package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.Behaviors

object InstanceProgress {

    const val COUNTING_RATIO: Double = 0.1

    fun determine(
        stableTimes: Long,
        states: List<InstanceState>,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>,
        timer: Behaviors.Timer
    ): String {
        return (timeout(timer.ticks, stableTimes) + " " + states(states, unavailableInstances, unstableInstances)).trim()
    }

    private fun timeout(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > COUNTING_RATIO)) {
            "!${maxTicks - tick}"
        } else {
            ""
        }
    }

    private fun states(states: List<InstanceState>, unavailableInstances: List<Instance>, unstableInstances: List<Instance>): String {
        return states.joinToString(" | ") {
            state(it, states.size > 2, unavailableInstances, unstableInstances)
        }
    }

    private fun state(
        state: InstanceState,
        shortProgress: Boolean,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>
    ): String {
        return "${state.instance.name} ${stateIndicator(state, unavailableInstances, unstableInstances)}|${stateDetails(state, shortProgress)}"
    }

    private fun stateIndicator(
        state: InstanceState,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>
    ): String {
        return when {
            unavailableInstances.contains(state.instance) -> "-"
            unstableInstances.contains(state.instance) -> "~"
            else -> "+"
        }
    }

    private fun stateDetails(state: InstanceState, shortInfo: Boolean): String {
        return if (shortInfo) {
            state.bundleState.stablePercent
        } else {
            "${state.bundleState.stablePercent}|${state.bundleState.statsWithLabels}"
        }
    }
}