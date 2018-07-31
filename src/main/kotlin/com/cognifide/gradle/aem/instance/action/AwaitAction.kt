package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.instance.ProgressLogger
import com.cognifide.gradle.aem.internal.ProgressCountdown
import org.apache.http.HttpStatus
import org.gradle.api.Project
import java.util.stream.Collectors

/**
 * Wait until all instances be stable.
 */
open class AwaitAction(project: Project, val instances: List<Instance>) : AbstractAction(project) {

    var fast = config.awaitFast

    var fastDelay = config.awaitFastDelay

    var resume = config.awaitResume

    var availableCheck = config.awaitAvailableCheck

    var stableTimes = config.awaitStableTimes

    var stableInterval = config.awaitStableInterval

    var stableState = config.awaitStableState

    var stableCheck = config.awaitStableCheck

    var stableAssurances = config.awaitStableAssurances

    var healthCheck = config.awaitHealthCheck

    override fun perform() {
        if (instances.isEmpty()) {
            logger.info("No instances to await.")
            return
        }

        if (fast) {
            awaitDelay()
        }

        awaitStable()

        if (!fast) {
            awaitHealthy()
        }
    }

    private fun awaitDelay() {
        ProgressCountdown(project, "Waiting for instance(s): ${instances.names}", fastDelay).run()
    }

    private fun awaitStable() {
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s): ${instances.names}", stableTimes)
        progressLogger.started()

        var lastStableChecksum = -1
        var sinceStableTicks = -1L

        val synchronizers = prepareSynchronizers()
        var unavailableNotification = false

        Behaviors.waitUntil(stableInterval) { timer ->
            // Gather all instance states (lazy)
            val instanceStates = synchronizers.map { it.determineInstanceState() }

            // Update checksum on any particular state change
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
            val unavailableInstances = synchronizers.map { it.instance } - availableInstances

            val initializedUnavailableInstances = unavailableInstances.filter { it.isInitialized(project) }
            if (!unavailableNotification && (timer.ticks.toDouble() / stableTimes.toDouble() > INSTANCE_UNAVAILABLE_RATIO) && initializedUnavailableInstances.isNotEmpty()) {
                notify("Instances not available", "Which: ${initializedUnavailableInstances.names}")
                unavailableNotification = true
            }

            progressLogger.progress(instanceStates, unavailableInstances, unstableInstances, timer)

            // Detect timeout when same checksum is not being updated so long
            if (stableTimes > 0 && timer.ticks > stableTimes) {
                instanceStates.forEach { it.status.logTo(logger) }

                if (!resume) {
                    throw InstanceException("Instances not stable: ${unstableInstances.names}. Timeout reached.")
                } else {
                    notify("Instances not stable", "Problem with: ${unstableInstances.names}. Timeout reached.")
                    return@waitUntil false
                }
            }

            if (unstableInstances.isEmpty()) {
                // Assure that expected moment is not accidental, remember it
                val assurable = (stableAssurances > 0) && (sinceStableTicks == -1L)
                if (!fast && assurable) {
                    logger.info("Instance(s) stable: ${instances.names}. Assuring.")
                    sinceStableTicks = timer.ticks
                }

                // End if assurance is not configured or this moment remains a little longer
                val assured = (stableAssurances <= 0) || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= stableAssurances)
                if (fast || assured) {
                    notify("Instance(s) stable", "Which: ${instances.names}", fast)
                    return@waitUntil false
                }
            } else {
                // Reset assurance, because no longer stable
                sinceStableTicks = -1L
            }

            true
        }

        progressLogger.completed()
    }

    private fun awaitHealthy() {
        logger.lifecycle("Checking health of instance(s): ${instances.names}")

        val synchronizers = prepareSynchronizers()
        for (i in 0..config.awaitHealthRetryTimes) {
            val instanceStates = synchronizers.map { it.determineInstanceState() }
            val unhealthyInstances = instanceStates.parallelStream().filter { !healthCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())

            if (unhealthyInstances.isEmpty()) {
                notify("Instance(s) healthy", "Which: ${instances.names}")
                return
            }

            if (i < config.awaitHealthRetryTimes) {
                logger.warn("Unhealthy instances detected: ${unhealthyInstances.names}")

                val header = "Retrying health check (${i + 1}/${config.awaitHealthRetryTimes}) after delay."
                val countdown = ProgressCountdown(project, header, config.awaitHealthRetryDelay)
                countdown.run()
            } else if (i == config.awaitHealthRetryTimes) {
                instanceStates.forEach { it.status.logTo(logger) }

                if (!resume) {
                    throw InstanceException("Instances not healthy: ${unhealthyInstances.names}.")
                } else {
                    notify("Instances not healthy", "Problem with: ${unhealthyInstances.names}.")
                }
            }
        }
    }

    private fun prepareSynchronizers(): List<InstanceSync> {
        return instances.map { instance ->
            val init = instance.isBeingInitialized(project)

            InstanceSync(project, instance).apply {
                val sync = this

                if (init) {
                    logger.debug("Initializing instance using default credentials.")
                    sync.basicUser = Instance.USER_DEFAULT
                    sync.basicPassword = Instance.PASSWORD_DEFAULT
                }

                responseHandler = { response ->
                    if (init) {
                        if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                            if (sync.basicUser == Instance.USER_DEFAULT) {
                                logger.debug("Switching instance credentials from defaults to customized.")
                                sync.basicUser = instance.user
                                sync.basicPassword = instance.password
                            } else {
                                logger.debug("Switching instance credentials from customized to defaults.")
                                sync.basicUser = Instance.USER_DEFAULT
                                sync.basicPassword = Instance.PASSWORD_DEFAULT
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val INSTANCE_UNAVAILABLE_RATIO: Double = 0.1
    }

}