package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.instance.LocalHandle
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.InstanceStateLogger
import org.gradle.api.Project
import java.util.stream.Collectors

class DownAction(project: Project, val instances: List<Instance>) : AbstractAction(project) {

    var stableTimes = config.awaitStableTimes

    var stableCheck = config.awaitStableCheck

    var availableCheck = config.awaitAvailableCheck

    val handles = instances.map { LocalHandle(project, it) }

    override fun perform() {
        if (instances.isEmpty()) {
            logger.info("No instances to shutdown.")
            return
        }

        shutDownInstances()
    }

    private fun shutDownInstances() {
        val stateLogger = InstanceStateLogger(project, "Awaiting instance(s) termination: ${instances.names}", stableTimes)
        stateLogger.started()

        val sync = instances.map { InstanceSync(project, it) }
        val instanceStates = sync.map { it.determineInstanceState() }
        var unavailableInstances = sync.map { it.instance }

        sendTerminateSignal()

        Behaviors.waitUntil(config.awaitStableInterval) { timer ->


            val unstableInstances = instanceStates.parallelStream()
                    .filter { !stableCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())

            val availableInstances = instanceStates.parallelStream()
                    .filter { availableCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())
            unavailableInstances -= availableInstances

            stateLogger.showState(instanceStates, unavailableInstances, unstableInstances, timer)

            handles.map { isProcessRunning(it) }
                    .reduce { sum, element -> sum && element }
        }

        notifier.default("Instance(s) down", "Which: ${handles.names}")
    }

    private fun sendTerminateSignal() = handles.parallelStream().forEach { it.down() }

    private fun isProcessRunning(it: LocalHandle): Boolean = it.pidFile.exists() && it.controlPortFile.exists()

}