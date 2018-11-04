package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.ProgressCountdown
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.http.HttpStatus
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * Wait until all instances be stable.
 */
open class AwaitAction(project: Project) : AbstractAction(project) {

    /**
     * Skip stable check assurances and health checking.
     */
    @Input
    var fast = aem.props.flag("aem.await.fast")

    /**
     * Time to wait e.g after deployment before checking instance stability.
     * Considered only when fast mode is enabled.
     */
    @Input
    var fastDelay = aem.props.long("aem.await.fast.delay", TimeUnit.SECONDS.toMillis(1))

    /**
     * Do not fail build but log warning when there is still some unstable or unhealthy instance.
     */
    @Input
    var resume: Boolean = aem.props.flag("aem.await.resume")

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
     * Number of intervals / additional instance stability checks to assure all stable instances.
     * This mechanism protect against temporary stable states.
     */
    @Input
    var stableAssurance: Long = aem.props.long("aem.await.stable.assurance", 3L)

    /**
     * Hook for customizing instance health check.
     */
    @Internal
    @get:JsonIgnore
    var healthCheck: (InstanceState) -> Boolean = { it.checkComponentState(10000) }

    /**
     * Repeat health check when failed (brute-forcing).
     */
    @Internal
    @get:JsonIgnore
    var healthRetry = aem.retry { afterSquaredSecond(aem.props.long("aem.await.health.retry", 6)) }

    override fun perform() {
        if (instances.isEmpty()) {
            aem.logger.info("No instances to await.")
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
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s): ${instances.names}", stableRetry.times)
        progressLogger.started()

        var lastStableChecksum = -1
        var sinceStableTicks = -1L

        val synchronizers = prepareSynchronizers()
        var unavailableNotification = false

        Behaviors.waitUntil(stableRetry.delay) { timer ->
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
            if (!unavailableNotification && (timer.ticks.toDouble() / stableRetry.times.toDouble() > INSTANCE_UNAVAILABLE_RATIO) && initializedUnavailableInstances.isNotEmpty()) {
                notify("Instances not available", "Which: ${initializedUnavailableInstances.names}")
                unavailableNotification = true
            }

            progressLogger.progress(instanceStates, unavailableInstances, unstableInstances, timer)

            // Detect timeout when same checksum is not being updated so long
            if (stableRetry.times > 0 && timer.ticks > stableRetry.times) {
                instanceStates.forEach { it.status.logTo(aem.logger) }

                if (!resume) {
                    throw InstanceException("Instances not stable: ${unstableInstances.names}. Timeout reached.")
                } else {
                    notify("Instances not stable", "Problem with: ${unstableInstances.names}. Timeout reached.")
                    return@waitUntil false
                }
            }

            if (unstableInstances.isEmpty()) {
                // Assure that expected moment is not accidental, remember it
                val assurable = (stableAssurance > 0) && (sinceStableTicks == -1L)
                if (!fast && assurable) {
                    aem.logger.info("Instance(s) stable: ${instances.names}. Assuring.")
                    sinceStableTicks = timer.ticks
                }

                // End if assurance is not configured or this moment remains a little longer
                val assured = (stableAssurance <= 0) || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= stableAssurance)
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
        aem.logger.lifecycle("Checking health of instance(s): ${instances.names}")

        val synchronizers = prepareSynchronizers()
        for (i in 0..healthRetry.times) {
            val instanceStates = synchronizers.map { it.determineInstanceState() }
            val unhealthyInstances = instanceStates.parallelStream().filter { !healthCheck(it) }
                    .map { it.instance }
                    .collect(Collectors.toList())

            if (unhealthyInstances.isEmpty()) {
                notify("Instance(s) healthy", "Which: ${instances.names}")
                return
            }

            if (i < healthRetry.times) {
                aem.logger.warn("Unhealthy instances detected: ${unhealthyInstances.names}")

                val header = "Retrying health check (${i + 1}/${healthRetry.times}) after delay."
                val countdown = ProgressCountdown(project, header, healthRetry.delay(i + 1))
                countdown.run()
            } else if (i == healthRetry.times) {
                instanceStates.forEach { it.status.logTo(aem.logger) }

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
                    aem.logger.debug("Initializing instance using default credentials.")
                    sync.basicUser = Instance.USER_DEFAULT
                    sync.basicPassword = Instance.PASSWORD_DEFAULT
                }

                responseHandler = { response ->
                    if (init) {
                        if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                            if (sync.basicUser == Instance.USER_DEFAULT) {
                                aem.logger.debug("Switching instance credentials from defaults to customized.")
                                sync.basicUser = instance.user
                                sync.basicPassword = instance.password
                            } else {
                                aem.logger.debug("Switching instance credentials from customized to defaults.")
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