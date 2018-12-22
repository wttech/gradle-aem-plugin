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
        return (ticks(timer.ticks, stableTimes) + " " + states.joinToString(" | ") {
            determine(it, states.size > 2, unavailableInstances, unstableInstances)
        }).trim()
    }

    private fun determine(
        state: InstanceState,
        shortProgress: Boolean,
        unavailableInstances: List<Instance>,
        unstableInstances: List<Instance>
    ): String {
        return "${state.instance.name} ${indicator(state, unavailableInstances, unstableInstances)}|${state(state, shortProgress)}"
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

    private fun indicator(
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

    private fun state(state: InstanceState, shortInfo: Boolean): String {
        return if (shortInfo) {
            state.bundleState.stablePercent
        } else {
            "${state.bundleState.stablePercent}|${state.bundleState.statsWithLabels}"
        }
    }
}