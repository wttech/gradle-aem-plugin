package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressCountdown
import com.cognifide.gradle.aem.internal.ProgressLogger
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

        logger.info("Awaiting instance(s): ${instances.names}")

        if (fast) {
            ProgressCountdown(project, "Awaiting instance(s)", fastDelay).run()
        }

        val progressLogger = ProgressLogger(project, "Awaiting instance(s)")
        progressLogger.started()

        var lastStableChecksum = -1
        var sinceStableTicks = -1L

        val synchronizers = prepareSynchronizers()
        var unavailableInstances = synchronizers.map { it.instance }
        var unavailableNotification = false

        Behaviors.waitUntil(stableInterval, { timer ->
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

            progressLogger.progress(progressFor(instanceStates, config, timer))

            // Detect unstable instances
            val unstableInstances = instanceStates.parallelStream()
                    .filter { !stableCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())

            // Detect unavailable instances
            val availableInstances = instanceStates.parallelStream()
                    .filter { availableCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())
            unavailableInstances -= availableInstances

            val initializedUnavailableInstances = unavailableInstances.filter { it.isInitialized(project) }
            if (!unavailableNotification && (timer.ticks.toDouble() / stableTimes.toDouble() > INSTANCE_UNAVAILABLE_RATIO) && initializedUnavailableInstances.isNotEmpty()) {
                notifier.default("Instances not available", "Which: ${initializedUnavailableInstances.names}")
                unavailableNotification = true
            }

            // Detect timeout when same checksum is not being updated so long
            if (stableTimes > 0 && timer.ticks > stableTimes) {
                if (!resume) {
                    throw InstanceException("Instances not stable: ${unstableInstances.names}. Timeout reached.")
                } else {
                    notifier.default("Instances not stable", "Problem with: ${unstableInstances.names}. Timeout reached.")
                    return@waitUntil false
                }
            }

            if (unstableInstances.isEmpty()) {
                // Skip assurance and health checking.
                if (fast) {
                    return@waitUntil false
                }

                // Assure that expected moment is not accidental, remember it
                if (stableAssurances > 0 && sinceStableTicks == -1L) {
                    progressLogger.progress("Instance(s) seems to be stable. Assuring.")
                    sinceStableTicks = timer.ticks
                }

                // End if assurance is not configured or this moment remains a little longer
                if (stableAssurances <= 0 || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= stableAssurances)) {
                    progressLogger.progress("Instance(s) are stable. Checking health.")

                    // Detect unhealthy instances
                    val unhealthyInstances = instanceStates.parallelStream()
                            .filter { !healthCheck(it) }
                            .map { it.instance }
                            .collect(Collectors.toList())

                    if (unhealthyInstances.isEmpty()) {
                        return@waitUntil false
                    } else {
                        if (!resume) {
                            throw InstanceException("Instances not healthy: ${unhealthyInstances.names}.")
                        } else {
                            notifier.default("Instances not healthy", "Problem with: ${unhealthyInstances.names}.")
                        }
                    }
                }
            } else {
                // Reset assurance, because no longer stable
                sinceStableTicks = -1L
            }

            true
        })

        progressLogger.completed()
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

    private fun progressFor(states: List<InstanceState>, config: AemConfig, timer: Behaviors.Timer): String {
        return (progressTicks(timer.ticks, config.awaitStableTimes) + " " + states.joinToString(" | ") { progressFor(it) }).trim()
    }

    private fun progressFor(state: InstanceState): String {
        return "${state.instance.name}: ${progressIndicator(state)} ${state.bundleState.statsWithLabels} [${state.bundleState.stablePercent}]"
    }

    private fun progressTicks(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > PROGRESS_COUNTING_RATIO)) {
            "[$tick/$maxTicks]"
        } else if (tick.rem(2) == 0L) {
            "[*]"
        } else {
            "[ ]"
        }
    }

    private fun progressIndicator(state: InstanceState): String {
        return if (stableCheck(state)) {
            "+"
        } else {
            "-"
        }
    }

    companion object {
        const val PROGRESS_COUNTING_RATIO: Double = 0.1

        const val INSTANCE_UNAVAILABLE_RATIO: Double = 0.1
    }

}