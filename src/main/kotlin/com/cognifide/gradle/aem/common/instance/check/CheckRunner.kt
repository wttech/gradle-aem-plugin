package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import kotlinx.coroutines.isActive
import org.apache.commons.lang3.time.StopWatch

class CheckRunner(internal val aem: AemExtension) {

    private var checks: CheckGroup.() -> List<Check> = { throw InstanceException("No instance checks defined!") }

    /**
     * Defines which checks should be performed (and repeated).
     */
    fun checks(definitions: CheckGroup.() -> List<Check>) {
        checks = definitions
    }

    /**
     * Controls how long to wait after failed checking before checking again.
     */
    var delay = 0L

    /**
     * Controls if aborted running should fail build.
     */
    var verbose = true

    /**
     * Measures running times.
     */
    private val runningWatch = StopWatch()

    /**
     * Time since running started.
     */
    val runningTime: Long
        get() = runningWatch.time

    /**
     * Time since last instance state change.
     */
    val stateTime: Long
        get() = runningWatch.splitTime

    /**
     * Error causing running stopped.
     */
    var abortCause: Exception? = null

    /**
     * Verify if running is stopped.
     */
    val aborted: Boolean
        get() = abortCause != null

    private val currentChecks = mutableMapOf<Instance, CheckGroup>()

    private var previousChecks = mapOf<Instance, CheckGroup>()

    val stateChanged: Boolean
        get() = currentChecks.any { (instance, current) ->
            val previous = previousChecks[instance] ?: return@any true
            current.state != previous.state
        }

    fun check(instances: Collection<Instance>) {
        aem.progressIndicator {
            updater = {
                val instanceSummaries = currentChecks.toSortedMap(compareBy { it.name })
                        .map { (instance, checks) -> "${instance.name}: ${checks.summary.decapitalize()}" }
                update(instanceSummaries.joinToString(" | "))
            }

            runningWatch.start()
            runningWatch.split()

            aem.parallel.each(instances) { instance ->
                while (isActive) {
                    val checks = CheckGroup(this@CheckRunner, instance, checks)
                    checks.check()

                    currentChecks[instance] = checks

                    if (aborted || checks.done) {
                        break
                    }

                    Behaviors.waitFor(delay)

                    previousChecks = currentChecks.toMap()
                    if (stateChanged) {
                        runningWatch.split()
                    }
                }
            }

            runningWatch.stop()

            if (aborted && verbose) {
                currentChecks.forEach { (_, group) ->
                    group.statusLogger.entries.forEach { aem.logger.log(it.level, it.details) }
                }

                abortCause?.let { throw it }
            }
        }
    }
}