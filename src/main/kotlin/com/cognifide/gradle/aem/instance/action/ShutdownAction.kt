package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Behaviors
import org.gradle.api.Project
import java.util.stream.Collectors

class ShutdownAction(project: Project, val instances: List<Instance>) : AbstractAction(project) {

    var stableTimes = config.awaitStableTimes

    var stableState = config.awaitStableState

    var stableCheck = config.awaitStableCheck

    var availableCheck = config.awaitAvailableCheck

    val handles = instances.map { LocalHandle(project, it) }

    override fun perform() {
        if (instances.isEmpty()) {
            logger.info("No instances to shutdown.")
            return
        }

        shutdown()
    }

    private fun shutdown() {
        val progressLogger = ProgressLogger(project, "Awaiting instance(s) shutdown: ${instances.names}", stableTimes)
        progressLogger.started()

        var lastStableChecksum = -1
        val instanceSynchronizers = handles.map { it.sync }

        handles.parallelStream().forEach { it.down() }

        Behaviors.waitUntil(config.awaitStableInterval) { timer ->
            // Update checksum on any particular state change
            val instanceStates = instanceSynchronizers.map { it.determineInstanceState() }
            val stableChecksum = instanceStates.parallelStream()
                    .map { stableState(it) }
                    .collect(Collectors.toList())
                    .hashCode()
            if (stableChecksum != lastStableChecksum) {
                lastStableChecksum = stableChecksum
                timer.reset()
            }

            // Examine instances
            val unstableInstances = instanceStates.parallelStream()
                    .filter { !stableCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())
            val availableInstances = instanceStates.parallelStream()
                    .filter { availableCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())
            val unavailableInstances = instanceSynchronizers.map { it.instance } - availableInstances
            val upInstances = handles.filter { it.running || availableInstances.contains(it.instance) }.map { it.instance }

            progressLogger.progress(instanceStates, unavailableInstances, unstableInstances, timer)

            // Detect timeout when same checksum is not being updated so long
            if (stableTimes > 0 && timer.ticks > stableTimes) {
                instanceStates.forEach { it.status.logTo(logger) }

                throw InstanceException("Instances cannot shutdown: ${upInstances.names}. Timeout reached.")
            }

            upInstances.isNotEmpty()
        }

        progressLogger.completed()

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

}