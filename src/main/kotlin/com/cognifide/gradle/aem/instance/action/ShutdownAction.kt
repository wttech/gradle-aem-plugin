package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.Behaviors
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceState
import com.cognifide.gradle.aem.instance.ProgressLogger
import com.cognifide.gradle.aem.instance.names
import com.fasterxml.jackson.annotation.JsonIgnore
import org.gradle.api.Project
import org.gradle.api.tasks.Internal

class ShutdownAction(project: Project) : AbstractAction(project) {

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    @Internal
    @get:JsonIgnore
    var stableRetry = aem.retry { afterSecond(aem.props.long("aem.shutdown.stableRetry") ?: 300) }

    /**
     * Hook for customizing instance state provider used within stable checking.
     * State change cancels actual assurance.
     */
    @Internal
    @get:JsonIgnore
    var stableState: InstanceState.() -> Int = { checkBundleState() }

    /**
     * Hook for customizing instance stability check.
     * Check will be repeated if assurance is configured.
     */
    @Internal
    @get:JsonIgnore
    var stableCheck: InstanceState.() -> Boolean = { checkBundleStable() }

    /**
     * Hook for customizing instance availability check.
     */
    @Internal
    @get:JsonIgnore
    var availableCheck: InstanceState.() -> Boolean = { check(InstanceState.BUNDLE_STATE_SYNC_OPTIONS, { !bundleState.unknown }) }

    override fun perform() {
        if (instances.isEmpty()) {
            aem.logger.info("No instances to shutdown.")
            return
        }

        shutdown()
    }

    private fun shutdown() {
        val progressLogger = ProgressLogger(project, "Awaiting instance(s) shutdown: ${instances.names}", stableRetry.times)
        progressLogger.started()

        var lastStableChecksum = -1

        aem.parallelWith(instanceHandles) { down() }

        Behaviors.waitUntil(stableRetry.delay) { timer ->
            // Update checksum on any particular state change
            val instanceStates = instances.map { it.sync.determineInstanceState() }
            val stableChecksum = aem.parallelMap(instanceStates) { stableState(it) }.hashCode()
            if (stableChecksum != lastStableChecksum) {
                lastStableChecksum = stableChecksum
                timer.reset()
            }

            // Examine instances
            val unstableInstances = aem.parallelMap(instanceStates, { !stableCheck(it) }, { it.instance })
            val availableInstances = aem.parallelMap(instanceStates, { availableCheck(it) }, { it.instance })
            val unavailableInstances = instances - availableInstances
            val upInstances = instanceHandles.filter { it.running || availableInstances.contains(it.instance) }.map { it.instance }

            progressLogger.progress(instanceStates, unavailableInstances, unstableInstances, timer)

            // Detect timeout when same checksum is not being updated so long
            if (stableRetry.times > 0 && timer.ticks > stableRetry.times) {
                instanceStates.forEach { it.status.logTo(aem.logger) }

                throw InstanceException("Instances cannot shutdown: ${upInstances.names}. Timeout reached.")
            }

            upInstances.isNotEmpty()
        }

        progressLogger.completed()
    }
}