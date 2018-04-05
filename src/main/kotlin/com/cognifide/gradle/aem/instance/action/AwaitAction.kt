package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceState
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.Project

class AwaitAction(project: Project, val instances: List<Instance>) : AbstractAction(project) {

    var times = config.awaitTimes

    var interval = config.awaitInterval

    var fail = config.awaitFail

    var condition = config.awaitCondition

    var timeout = config.awaitTimeout

    var assurances = config.awaitAssurances

    override fun perform() {
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")

        progressLogger.started()

        logger.info("Checking stability of instance(s).")

        var lastInstanceStates = -1
        var sinceStableTicks = -1L
        var sinceStableElapsed = 0L

        Behaviors.waitUntil(interval, { timer ->
            // Gather all instance states and update checksum on any particular state change
            val instanceStates = instances.map { InstanceState(project, it, timeout) }
            if (instanceStates.hashCode() != lastInstanceStates) {
                lastInstanceStates = instanceStates.hashCode()
                timer.reset()
            }

            progressLogger.progress(progressFor(instanceStates, timer))

            // Detect timeout when same checksum is not being updated so long
            if (times > 0 && timer.ticks > times) {
                val message = "Instance(s) are not stable. Timeout reached after ${Formats.duration(timer.elapsed)}."
                if (fail) {
                    throw InstanceException(message)
                } else {
                    logger.warn(message)
                    return@waitUntil false
                }
            }

            // Verify gathered instance states
            if (instanceStates.all(condition)) {
                // Assure that expected moment is not accidental, remember it
                if (assurances > 0 && sinceStableTicks == -1L) {
                    logger.info("Instance(s) seems to be stable. Assuring.")
                    sinceStableTicks = timer.ticks
                    sinceStableElapsed = timer.elapsed
                }

                // End if assurance is not configured or this moment remains a little longer
                if (assurances <= 0 || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= assurances)) {
                    logger.info("Instance(s) are stable after ${Formats.duration(sinceStableElapsed)}.")
                    return@waitUntil false
                }
            } else {
                // Reset assurance, because no longer verified
                sinceStableTicks = -1L
                sinceStableElapsed = 0L
            }

            true
        })

        progressLogger.completed()
    }

    private fun progressFor(states: List<InstanceState>, timer: Behaviors.Timer) =
            (progressTicks(timer.ticks, times) + " " + states.joinToString(" | ") { progressFor(it, timer.ticks) }).trim()
            (progressTicks(timer.ticks, 0) + " " + states.joinToString(" | ") { progressFor(it) }).trim()

    private fun progressFor(states: List<InstanceState>, config: AemConfig, timer: Behaviors.Timer) =
            (progressTicks(timer.ticks, config.awaitTimes) + " " + states.joinToString(" | ") { progressFor(it) }).trim()

    private fun progressFor(state: InstanceState): String {
        return "${state.instance.name}: ${progressIndicator(state)} ${state.bundleState.statsWithLabels} [${state.bundleState.stablePercent}]"
    }

    private fun progressTicks(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > 0.1)) {
            "[$tick/$maxTicks]"
        } else if (tick.rem(2) == 0L) {
            "[*]"
        } else {
            "[ ]"
        }
    }

    private fun progressIndicator(state: InstanceState): String {
        return if (state.config.awaitCondition(state)) {
            "+"
        } else {
            "-"
        }
    }

}