package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.instance.*
import com.cognifide.gradle.aem.internal.Behaviors
import com.cognifide.gradle.aem.internal.Formats
import com.cognifide.gradle.aem.internal.ProgressLogger
import org.apache.http.HttpStatus
import org.apache.http.client.config.RequestConfig
import org.gradle.api.Project

/**
 * Wait until all instances be stable.
 */
open class AwaitAction(project: Project, val instances: List<Instance>) : AbstractAction(project) {

    var times = config.awaitTimes

    var interval = config.awaitInterval

    var fail = config.awaitFail

    var condition = config.awaitCondition

    var timeout = config.awaitTimeout

    var assurances = config.awaitAssurances

    override fun perform() {
        val progressLogger = ProgressLogger(project, "Awaiting stable instance(s)")

        progressLogger.started()

        logger.info("Checking stability of instance(s).")

        var lastInstanceStates = -1
        var sinceStableTicks = -1L
        var sinceStableElapsed = 0L

        val synchronizers = prepareSynchronizers()

        Behaviors.waitUntil(interval, { timer ->
            // Gather all instance states and update checksum on any particular state change
            val instanceStates = synchronizers.map { it.determineInstanceState() }
            if (instanceStates.hashCode() != lastInstanceStates) {
                lastInstanceStates = instanceStates.hashCode()
                timer.reset()
            }

            progressLogger.progress(progressFor(instanceStates, config, timer))

            // Detect timeout when same checksum is not being updated so long
            if (times > 0 && timer.ticks > times) {
                val message = "Instance(s) are not stable. Timeout reached after ${Formats.duration(timer.elapsed)}."
                if (fail) {
                    throw InstanceException(message)
                } else {
                    logger.warn(message)
                    return@waitUntil false
                }
            }

            // Verify gathered instance states
            if (instanceStates.all(condition)) {
                // Assure that expected moment is not accidental, remember it
                if (assurances > 0 && sinceStableTicks == -1L) {
                    logger.info("Instance(s) seems to be stable. Assuring.")
                    sinceStableTicks = timer.ticks
                    sinceStableElapsed = timer.elapsed
                }

                // End if assurance is not configured or this moment remains a little longer
                if (assurances <= 0 || (sinceStableTicks >= 0 && (timer.ticks - sinceStableTicks) >= assurances)) {
                    logger.info("Instance(s) are stable after ${Formats.duration(sinceStableElapsed)}.")
                    return@waitUntil false
                }
            } else {
                // Reset assurance, because no longer verified
                sinceStableTicks = -1L
                sinceStableElapsed = 0L
            }

            true
        })

        progressLogger.completed()
    }

    private fun prepareSynchronizers(): List<InstanceSync> {
        return instances.map {
            InstanceSync(project, it).apply {
                val sync = this

                requestConfigurer = { request ->
                    request.config = RequestConfig.custom()
                            .setConnectTimeout(timeout)
                            .setSocketTimeout(timeout)
                            .build()
                }
                responseHandler = { response ->
                    if (instance is LocalInstance && !LocalHandle(project, instance).initialized) {
                        if (response.statusLine.statusCode == HttpStatus.SC_UNAUTHORIZED) {
                            if (sync.basicUser == Instance.USER_DEFAULT) {
                                sync.basicUser = instance.user
                                sync.basicPassword = instance.password
                            } else {
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
        return (progressTicks(timer.ticks, config.awaitTimes) + " " + states.joinToString(" | ") { progressFor(it) }).trim()
    }

    private fun progressFor(state: InstanceState): String {
        return "${state.instance.name}: ${progressIndicator(state)} ${state.bundleState.statsWithLabels} [${state.bundleState.stablePercent}]"
    }

    private fun progressTicks(tick: Long, maxTicks: Long): String {
        return if (maxTicks > 0 && (tick.toDouble() / maxTicks.toDouble() > 0.1)) {
            "[$tick/$maxTicks]"
        } else if (tick.rem(2) == 0L) {
            "[*]"
        } else {
            "[ ]"
        }
    }

    private fun progressIndicator(state: InstanceState): String {
        return if (condition(state)) {
            "+"
        } else {
            "-"
        }
    }

}