package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceState
import com.cognifide.gradle.aem.instance.ProgressLogger
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.internal.Behaviors
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import org.gradle.api.tasks.Internal
import java.util.stream.Collectors

class ShutdownAction(project: Project) : AbstractAction(project) {

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    @Internal
    @get:JsonIgnore
    var stableRetry = aem.retry { afterSecond(aem.props.long("aem.await.stable.retry", 300)) }

    /**
     * Hook for customizing instance state provider used within stable checking.
     * State change cancels actual assurance.
     */
    @Internal
    @get:JsonIgnore
    var stableState: (InstanceState) -> Int = { it.checkBundleState(500) }

    /**
     * Hook for customizing instance stability check.
     * Check will be repeated if assurance is configured.
     */
    @Internal
    @get:JsonIgnore
    var stableCheck: (InstanceState) -> Boolean = { it.checkBundleStable(500) }

    /**
     * Hook for customizing instance availability check.
     */
    @Internal
    @get:JsonIgnore
    var availableCheck: (InstanceState) -> Boolean = { state ->
        state.check({ sync ->
            sync.connectionTimeout = 750
            sync.connectionRetries = false
        }, {
            !state.bundleState.unknown
        })
    }

    override fun perform() {
        if (instances.isEmpty()) {
            logger.info("No instances to shutdown.")
            return
        }

        shutdown()
    }

    private fun shutdown() {
        val progressLogger = ProgressLogger(project, "Awaiting instance(s) shutdown: ${instances.names}", stableRetry.times)
        progressLogger.started()

        var lastStableChecksum = -1
        val instanceSynchronizers = handles.map { it.sync }

        handles.parallelStream().forEach { it.down() }

        Behaviors.waitUntil(stableRetry.delay) { timer ->
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
            if (stableRetry.times > 0 && timer.ticks > stableRetry.times) {
                instanceStates.forEach { it.status.logTo(logger) }

                throw InstanceException("Instances cannot shutdown: ${upInstances.names}. Timeout reached.")
            }

            upInstances.isNotEmpty()
        }

        progressLogger.completed()
    }

}