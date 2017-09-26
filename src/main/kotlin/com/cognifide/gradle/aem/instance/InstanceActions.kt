package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.gradle.api.Project

object InstanceActions {

    fun awaitStable(project: Project, instances: List<Instance> = Instance.locals(project)) {
        val config = AemConfig.of(project)
        val logger = project.logger

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
        Behaviors.waitUntil(config.awaitInterval, { timer ->
            val instanceStates = instances.map { InstanceState(project, it) }
            if (instanceStates.hashCode() != lastInstanceStates) {
                lastInstanceStates = instanceStates.hashCode()
                timer.reset()
            }

            progressLogger.progress(progressFor(instanceStates, config, timer))

            if (config.awaitTimes > 0 && timer.ticks > config.awaitTimes) {
                logger.warn("Instance(s) are not stable. Timeout reached after ${Formats.duration(timer.elapsed)}")
                return@waitUntil false
            }

            if (instanceStates.all { it.stable }) {
                logger.info("Instance(s) are stable after ${Formats.duration(timer.elapsed)}")
                return@waitUntil false
            }

            true
        })

        progressLogger.completed()
    }

    private fun progressFor(instanceStates: List<InstanceState>, timer: Behaviors.Timer) =
            instanceStates.joinToString(" | ") { progressFor(it, timer.ticks, 0) }

    private fun progressFor(instanceStates: List<InstanceState>, config: AemConfig, timer: Behaviors.Timer) =
            instanceStates.joinToString(" | ") { progressFor(it, timer.ticks, config.awaitTimes) }

    private fun progressFor(it: InstanceState, tick: Long, maxTicks: Long): String {
        return "${it.instance.name}: ${progressIndicator(it, tick, maxTicks)} ${it.bundleState.statsWithLabels} [${it.bundleState.stablePercent}]"
    }

    private fun progressIndicator(state: InstanceState, tick: Long, maxTicks: Long): String {
        var indicator = if (state.stable || tick.rem(2) == 0L) {
            "*"
        } else {
            " "
        }

        if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > 0.1)) {
            indicator += " [$tick/$maxTicks tt]"
        }

        return indicator
    }

}
