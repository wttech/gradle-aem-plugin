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

        if (config.instanceAwaitDelay > 0) {
            logger.info("Delaying instance stability checking")
            Behaviors.waitFor(config.instanceAwaitDelay)
        }

        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")

        progressLogger.started()

        var lastInstanceStates = -1
        Behaviors.waitUntil(config.instanceAwaitInterval, { timer ->
            if (config.instanceAwaitTimes > 0 && timer.ticks > config.instanceAwaitTimes) {
                logger.warn("Instance(s) are not stable. Timeout reached after ${Formats.duration(timer.elapsed)}")
                return@waitUntil false
            }

            val instanceStates = instances.map { InstanceState(project, it) }
            if (instanceStates.hashCode() != lastInstanceStates) {
                lastInstanceStates = instanceStates.hashCode()
                timer.reset()
            }

            val instanceProgress = instanceStates.joinToString(" | ") { progressFor(it, timer.ticks, config.instanceAwaitTimes) }
            progressLogger.progress(instanceProgress)

            if (instanceStates.all { it.stable }) {
                logger.info("Instance(s) are stable after ${Formats.duration(timer.elapsed)}")
                return@waitUntil false
            }

            true
        })

        progressLogger.completed()
    }

    private fun progressFor(it: InstanceState, tick: Long, maxTicks: Long): String {
        return "${it.instance.name}: ${progressIndicator(it, tick, maxTicks)} ${it.bundleState.statsWithLabels} [${it.bundleState.stablePercent}]"
    }

    private fun progressIndicator(state: InstanceState, tick: Long, maxTicks: Long): String {
        var indicator = if (state.stable || tick.rem(2) == 0L) {
            "*"
        } else {
            " "
        }

        if (tick.toDouble() / maxTicks.toDouble() > 0.1) {
            indicator += " [$tick/$maxTicks tt]"
        }

        return indicator
    }

}
