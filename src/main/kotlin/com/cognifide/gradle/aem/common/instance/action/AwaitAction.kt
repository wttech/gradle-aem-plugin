package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.build.ProgressCountdown
import com.cognifide.gradle.aem.common.build.ProgressLogger
import com.cognifide.gradle.aem.common.instance.*
import com.cognifide.gradle.aem.common.instance.service.StateChecker
import com.cognifide.gradle.aem.common.utils.Formats
import java.util.concurrent.TimeUnit
import org.apache.http.HttpStatus

/**
 * Wait until all instances be stable.
 */
open class AwaitAction(aem: AemExtension) : AbstractAction(aem) {

    /**
     * Skip stable check assurances and health checking.
     */
    var fast = aem.props.flag("instance.await.fast")

    /**
     * Time to wait if check assurances and health checking are skipped.
     */
    var fastDelay = aem.props.long("instance.await.fastDelay") ?: TimeUnit.SECONDS.toMillis(1)

    /**
     * Time to wait e.g after deployment before checking instance stability.
     * Considered only when fast mode is disabled.
     */
    var warmupDelay = aem.props.long("instance.await.warmupDelay") ?: TimeUnit.SECONDS.toMillis(0)

    /**
     * Do not fail build but log warning when there is still some unstable or unhealthy instance.
     */
    var resume: Boolean = aem.props.flag("instance.await.resume")

    /**
     * Hook for customizing instance availability check.
     */
    var availableCheck: StateChecker.() -> Boolean = {
        check(StateChecker.BUNDLE_STATE_SYNC_OPTIONS, { !bundleState.unknown })
    }

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    var stableRetry = aem.retry { afterSecond(aem.props.long("instance.await.stableRetry") ?: 300) }

    /**
     * Hook for customizing instance state provider used within stable checking.
     * State change cancels actual assurance.
     */
    var stableState: StateChecker.() -> Int = { checkState() }

    /**
     * Hook for customizing instance stability check.
     * Check will be repeated if assurance is configured.
     */
    var stableCheck: StateChecker.() -> Boolean = { checkStable() }

    /**
     * Number of intervals / additional instance stability checks to assure all stable instances.
     * This mechanism protect against temporary stable states.
     */
    var stableAssurance: Long = aem.props.long("instance.await.stableAssurance") ?: 3L

    /**
     * Hook for customizing instance health check.
     */
    var healthCheck: StateChecker.() -> Boolean = {
        checkComponentState(StateChecker.PLATFORM_COMPONENTS, aem.javaPackages.map { "$it.*" })
    }

    /**
     * Repeat health check when failed (brute-forcing).
     */
    var healthRetry = aem.retry { afterSquaredSecond(aem.props.long("instance.await.healthRetry") ?: 5) }

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to await.")
            return
        }

        if (fast) {
            awaitDelay(fastDelay)
        } else {
            awaitDelay(warmupDelay)
        }

        awaitStable()

        if (!fast) {
            awaitHealthy()
        }
    }

    private fun awaitDelay(delay: Long) {
        if (delay <= 0) {
            return
        }

        aem.logger.info("Waiting for instance(s): ${instances.names}")

        ProgressCountdown(aem.project, delay).run()
    }

    @Suppress("ComplexMethod")
    private fun awaitStable() {
        aem.logger.info("Awaiting stable instance(s): ${instances.names}")

        ProgressLogger.of(aem.project).launch {
            var lastStableChecksum = -1
            var sinceStableTicks = -1L

            val synchronizers = prepareSynchronizers()
            var unavailableNotification = false

            Behaviors.waitUntil(stableRetry.delay) { timer ->
                // Gather all instance states (lazy)
                val instanceStates = synchronizers.map { it.stateChecker() }

                // Update checksum on any particular state change
                val stableChecksum = aem.parallel.map(instanceStates) { stableState(it) }.hashCode()
                if (stableChecksum != lastStableChecksum) {
                    lastStableChecksum = stableChecksum
                    timer.reset()
                }

                // Examine instances
                val unstableInstances = aem.parallel.map(instanceStates, { !stableCheck(it) }, { it.instance })
                val availableInstances = aem.parallel.map(instanceStates, { availableCheck(it) }, { it.instance })
                val unavailableInstances = synchronizers.map { it.instance } - availableInstances

                val initializedUnavailableInstances = unavailableInstances.filter { it.isInitialized() }
                val areUnavailableInstances = (timer.ticks.toDouble() / stableRetry.times.toDouble() > INSTANCE_UNAVAILABLE_RATIO) &&
                        initializedUnavailableInstances.isNotEmpty()

                if (!unavailableNotification && areUnavailableInstances) {
                    notify("Instances not available", "Which: ${initializedUnavailableInstances.names}")
                    unavailableNotification = true
                }

                progress(InstanceProgress.determine(stableRetry.times, instanceStates, unavailableInstances, unstableInstances, timer))

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
        }
    }

    private fun awaitHealthy() {
        aem.logger.info("Checking health of instance(s): ${instances.names}")

        val synchronizers = prepareSynchronizers()
        for (i in 0..healthRetry.times) {
            val instanceStates = synchronizers.map { it.stateChecker() }
            val unhealthyInstances = aem.parallel.map(instanceStates, { !healthCheck(it) }, { it.instance })
            if (unhealthyInstances.isEmpty()) {
                notify("Instance(s) healthy", "Which: ${instances.names}")
                return
            }

            if (i < healthRetry.times) {
                aem.logger.warn("Unhealthy instances detected: ${unhealthyInstances.names}")

                val delay = healthRetry.delay(i + 1)
                val countdown = ProgressCountdown(aem.project, delay)

                aem.logger.lifecycle("Retrying health check (${i + 1}/${healthRetry.times}) after delay: ${Formats.duration(delay)}")
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
            val init = instance.isBeingInitialized()

            instance.sync.also { sync ->
                if (init) {
                    aem.logger.debug("Initializing instance using default credentials.")
                    sync.http.basicUser = Instance.USER_DEFAULT
                    sync.http.basicPassword = Instance.PASSWORD_DEFAULT
                }

                sync.http.responseHandler = { response ->
                    if (init && response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                        if (sync.http.basicUser == Instance.USER_DEFAULT) {
                            aem.logger.debug("Switching instance credentials from defaults to customized.")
                            sync.http.basicUser = instance.user
                            sync.http.basicPassword = instance.password
                        } else {
                            aem.logger.debug("Switching instance credentials from customized to defaults.")
                            sync.http.basicUser = Instance.USER_DEFAULT
                            sync.http.basicPassword = Instance.PASSWORD_DEFAULT
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