package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Behaviors
import com.cognifide.gradle.aem.common.ProgressLogger
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceProgress
import com.cognifide.gradle.aem.instance.names
import com.cognifide.gradle.aem.instance.service.StateChecker

class ShutdownAction(aem: AemExtension) : AbstractAction(aem) {

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    var stableRetry = aem.retry { afterSecond(aem.props.long("instance.shutdown.stableRetry") ?: 300) }

    /**
     * Hook for customizing instance state provider used within stable checking.
     * State change cancels actual assurance.
     */
    var stableState: StateChecker.() -> Int = { checkBundleState() }

    /**
     * Hook for customizing instance stability check.
     * Check will be repeated if assurance is configured.
     */
    var stableCheck: StateChecker.() -> Boolean = { checkBundleStable() }

    /**
     * Hook for customizing instance availability check.
     */
    var availableCheck: StateChecker.() -> Boolean = { check(StateChecker.BUNDLE_STATE_SYNC_OPTIONS, { !bundleState.unknown }) }

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
        aem.logger.info("Awaiting instance(s) shutdown: ${instances.names}")

        ProgressLogger.of(aem.project).launch {
            var lastStableChecksum = -1

            aem.parallel.with(localInstances) { down() }

            Behaviors.waitUntil(stableRetry.delay) { timer ->
                // Update checksum on any particular state change
                val instanceStates = instances.map { it.sync.stateChecker() }
                val stableChecksum = aem.parallel.map(instanceStates) { stableState(it) }.hashCode()
                if (stableChecksum != lastStableChecksum) {
                    lastStableChecksum = stableChecksum
                    timer.reset()
                }

                // Examine instances
                val unstableInstances = aem.parallel.map(instanceStates, { !stableCheck(it) }, { it.instance })
                val availableInstances = aem.parallel.map(instanceStates, { availableCheck(it) }, { it.instance })
                val unavailableInstances = instances - availableInstances
                val upInstances = localInstances.filter { it.running || availableInstances.contains(it) }

                progress(InstanceProgress.determine(stableRetry.times, instanceStates, unavailableInstances, unstableInstances, timer))

                // Detect timeout when same checksum is not being updated so long
                if (stableRetry.times > 0 && timer.ticks > stableRetry.times) {
                    instanceStates.forEach { it.status.logTo(aem.logger) }

                    throw InstanceException("Instances cannot shutdown: ${upInstances.names}. Timeout reached.")
                }

                upInstances.isNotEmpty()
            }
        }
    }
}