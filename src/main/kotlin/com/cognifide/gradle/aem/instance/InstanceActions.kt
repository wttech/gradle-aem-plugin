package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.Project

class InstanceActions(val project: Project) {

    val config = AemConfig.of(project)

    val logger = project.logger

    fun awaitStable(instances: List<Instance>) {
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")

        progressLogger.started()

        // Check if delay is configured
        if (config.awaitDelay > 0) {
            logger.info("Delaying due to pending operations on instance(s).")

            Behaviors.waitUntil(config.awaitInterval, { timer ->
                val instanceStates = instances.map { InstanceState(project, it) }
                progressLogger.progress(progressFor(instanceStates, timer))

                return@waitUntil (timer.elapsed < config.awaitDelay)
            })
        }

        logger.info("Checking stability of instance(s).")

        var lastInstanceStates = -1
        var sinceStableTicks = -1L
        var sinceStableElapsed = 0L

        Behaviors.waitUntil(config.awaitInterval, { timer ->
            // Gather all instance states and update checksum on any particular state change
            val instanceStates = instances.map { InstanceState(project, it) }
            if (instanceStates.hashCode() != lastInstanceStates) {
                lastInstanceStates = instanceStates.hashCode()
                timer.reset()
            }

            progressLogger.progress(progressFor(instanceStates, config, timer))

            // Detect timeout when same checksum is not being updated so long
            if (config.awaitTimes > 0 && timer.ticks > config.awaitTimes) {
                val message = "Instance(s) are not stable. Timeout reached after ${Formats.duration(timer.elapsed)}."
                if (config.awaitFail) {
                    throw InstanceException(message)
                } else {
                    logger.warn(message)
                    return@waitUntil false
                }
            }

            // Verify gathered instance states
            if (instanceStates.all(config.awaitCondition)) {
                // Assure that expected moment is not accidental, remember it
                if (config.awaitAssurances > 0 && sinceStableTicks == -1L) {
                    logger.info("Instance(s) seems to be stable. Assuring.")
                    sinceStableTicks = timer.ticks
                    sinceStableElapsed = timer.elapsed
                }

                // End if assurance is not configured or this moment remains a little longer
                if (config.awaitAssurances <= 0 || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= config.awaitAssurances)) {
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
            (progressTicks(timer.ticks, 0) + " " + states.joinToString(" | ") { progressFor(it, timer.ticks) }).trim()

    private fun progressFor(states: List<InstanceState>, config: AemConfig, timer: Behaviors.Timer) =
            (progressTicks(timer.ticks, config.awaitTimes) + " " + states.joinToString(" | ") { progressFor(it, timer.ticks) }).trim()

    private fun progressFor(state: InstanceState, tick: Long): String {
        return "${state.instance.name}: ${progressIndicator(state, tick)} ${state.bundleState.statsWithLabels} [${state.bundleState.stablePercent}]"
    }

    private fun progressTicks(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > 0.1)) {
            "[$tick/$maxTicks tt]"
        } else {
            ""
        }
    }

    private fun progressIndicator(state: InstanceState, tick: Long): String {
        var indicator = if (tick.rem(2) == 0L) {
            if (state.config.awaitCondition(state)) {
                "+"
            } else {
                "-"
            }
        } else {
            " "
        }



        return indicator
    }

}
