package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.*
import com.cognifide.gradle.aem.instance.*
import java.util.concurrent.TimeUnit
import org.apache.http.HttpStatus

/**
 * Wait until all instances be stable.
 */
open class AwaitAction(aem: AemExtension) : AbstractAction(aem) {

    /**
     * Skip stable check assurances and health checking.
     */
    var fast = aem.props.flag("await.fast")

    /**
     * Time to wait if check assurances and health checking are skipped.
     */
    var fastDelay = aem.props.long("await.fastDelay") ?: TimeUnit.SECONDS.toMillis(1)

    /**
     * Time to wait e.g after deployment before checking instance stability.
     * Considered only when fast mode is disabled.
     */
    var warmupDelay = aem.props.long("await.warmupDelay") ?: TimeUnit.SECONDS.toMillis(0)

    /**
     * Do not fail build but log warning when there is still some unstable or unhealthy instance.
     */
    var resume: Boolean = aem.props.flag("await.resume")

    /**
     * Hook for customizing instance availability check.
     */
    var availableCheck: InstanceState.() -> Boolean = {
        check(InstanceState.BUNDLE_STATE_SYNC_OPTIONS, { !bundleState.unknown })
    }

    /**
     * Maximum intervals after which instance stability checks will
     * be skipped if there is still some unstable instance left.
     */
    var stableRetry = aem.retry { afterSecond(aem.props.long("await.stableRetry") ?: 300) }

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
     * Number of intervals / additional instance stability checks to assure all stable instances.
     * This mechanism protect against temporary stable states.
     */
    var stableAssurance: Long = aem.props.long("await.stableAssurance") ?: 3L

    /**
     * Hook for customizing instance health check.
     */
    var healthCheck: InstanceState.() -> Boolean = {
        checkComponentState(InstanceState.PLATFORM_COMPONENTS, aem.javaPackages.map { "$it.*" })
    }

    /**
     * Repeat health check when failed (brute-forcing).
     */
    var healthRetry = aem.retry { afterSquaredSecond(aem.props.long("await.healthRetry") ?: 5) }

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
                val instanceStates = synchronizers.map { it.determineInstanceState() }

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
            val instanceStates = synchronizers.map { it.determineInstanceState() }
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

            instance.sync.apply {
                val sync = this

                if (init) {
                    aem.logger.debug("Initializing instance using default credentials.")
                    sync.basicUser = Instance.USER_DEFAULT
                    sync.basicPassword = Instance.PASSWORD_DEFAULT
                }

                responseHandler = { response ->
                    if (init && response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
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

    companion object {
        const val INSTANCE_UNAVAILABLE_RATIO: Double = 0.1
    }
}