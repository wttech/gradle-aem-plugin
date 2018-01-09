package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.Project

class InstanceActions(val project: Project) {

    val config = AemConfig.of(project)

    val logger = project.logger

    fun awaitStable(instances: List<Instance> = Instance.locals(project)) {
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")

        progressLogger.started()

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
            val instanceStates = instances.map { InstanceState(project, it) }
            if (instanceStates.hashCode() != lastInstanceStates) {
                lastInstanceStates = instanceStates.hashCode()
                timer.reset()
            }

            progressLogger.progress(progressFor(instanceStates, config, timer))

            if (config.awaitTimes > 0 && timer.ticks > config.awaitTimes) {
                logger.warn("Instance(s) are not stable. Timeout reached after ${Formats.duration(timer.elapsed)}.")
                return@waitUntil false
            }

            if (instanceStates.all(config.awaitCondition)) {
                if (config.awaitAssurances > 0 && sinceStableTicks == -1L) {
                    logger.info("Instance(s) seems to be stable. Assuring.")
                    sinceStableTicks = timer.ticks
                    sinceStableElapsed = timer.elapsed
                }

                if (config.awaitAssurances <= 0 || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= config.awaitAssurances)) {
                    logger.info("Instance(s) are stable after ${Formats.duration(sinceStableElapsed)}.")
                    return@waitUntil false
                }
            } else {
                sinceStableTicks = -1L
            }

            true
        })

        progressLogger.completed()
    }

    private fun progressFor(states: List<InstanceState>, timer: Behaviors.Timer) =
            states.joinToString(" | ") { progressFor(it, timer.ticks, 0) }

    private fun progressFor(states: List<InstanceState>, config: AemConfig, timer: Behaviors.Timer) =
            states.joinToString(" | ") { progressFor(it, timer.ticks, config.awaitTimes) }

    private fun progressFor(state: InstanceState, tick: Long, maxTicks: Long): String {
        return "${state.instance.name}: ${progressIndicator(state, tick, maxTicks)} ${state.bundleState.statsWithLabels} [${state.bundleState.stablePercent}]"
    }

    private fun progressIndicator(state: InstanceState, tick: Long, maxTicks: Long): String {
        var indicator = if (tick.rem(2) == 0L) {
            if (state.config.awaitCondition(state)) {
                "+"
            } else {
                "-"
            }
        } else {
            " "
        }

        if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > 0.1)) {
            indicator += " [$tick/$maxTicks tt]"
        }

        return indicator
    }

}
