package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Behaviors
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceState
import com.cognifide.gradle.aem.instance.ProgressLogger
import com.cognifide.gradle.aem.instance.names

class ShutdownAction(aem: AemExtension) : AbstractAction(aem) {

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    var stableRetry = aem.retry { afterSecond(aem.props.long("aem.shutdown.stableRetry") ?: 300) }

    /**
     * Hook for customizing instance state provider used within stable checking.
     * State change cancels actual assurance.
     */
    var stableState: InstanceState.() -> Int = { checkBundleState() }

    /**
     * Hook for customizing instance stability check.
     * Check will be repeated if assurance is configured.
     */
    var stableCheck: InstanceState.() -> Boolean = { checkBundleStable() }

    /**
     * Hook for customizing instance availability check.
     */
    var availableCheck: InstanceState.() -> Boolean = { check(InstanceState.BUNDLE_STATE_SYNC_OPTIONS, { !bundleState.unknown }) }

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to shutdown.")
            return
        }

        shutdown()
    }

    private fun shutdown() {
        val progressLogger = ProgressLogger(aem.project, "Awaiting instance(s) shutdown: ${instances.names}", stableRetry.times)
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