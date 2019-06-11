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
     * How long to wait before running checks.
     */
    var wait = 0L

    /**
     * How long to wait after failed checking before checking again.
     */
    var delay = 0L

    /**
     * Controls if aborted running should fail build.
     */
    var verbose = true

    /**
     * Time since running started.
     */
    val runningTime: Long
        get() = runningWatch.time

    private val runningWatch = StopWatch()

    /**
     * Error causing running stopped.
     */
    var abortCause: Exception? = null

    /**
     * Verify if running is stopped.
     */
    val aborted: Boolean
        get() = abortCause != null

    /**
     * Controls logging behavior
     *
     * By default it just keeps clean console if info logging level is not enabled.
     */
    var logInstantly = aem.logger.isInfoEnabled

    private val currentChecks = mutableMapOf<Instance, CheckGroup>()

    private var previousChecks = mapOf<Instance, CheckGroup>()

    private var stateWatches = mutableMapOf<Instance, StopWatch>()

    @Suppress("ComplexMethod")
    fun check(instances: Collection<Instance>) {
        aem.progressIndicator {
            updater = {
                val instanceSummaries = currentChecks.toSortedMap(compareBy { it.name })
                        .map { (instance, checks) -> "${instance.name}: ${checks.summary}" }
                update(instanceSummaries.joinToString(" | "))
            }

            step = "Waiting"
            Behaviors.waitFor(wait)

            step = "Checking"

            runningWatch.start()

            aem.parallel.each(instances) { instance ->
                stateWatches[instance] = StopWatch().apply { start() }

                do {
                    val checks = CheckGroup(this@CheckRunner, instance, checks).apply {
                        check()
                        if (logInstantly) {
                            log()
                        }
                    }

                    currentChecks[instance] = checks

                    if (stateChanged(instance)) {
                        stateWatches[instance]?.apply { reset(); start() }
                    }

                    previousChecks = currentChecks.toMap()

                    if (checks.done || aborted) {
                        break
                    }

                    Behaviors.waitFor(delay)
                } while (isActive)
            }

            runningWatch.stop()

            step = "Aborting"

            if (aborted && verbose) {
                if (!logInstantly) {
                    currentChecks.values.forEach { it.log() }
                }
                abortCause?.let { throw it }
            }
        }
    }

    fun stateChanged(instance: Instance): Boolean {
        val current = currentChecks[instance] ?: return true
        val previous = previousChecks[instance] ?: return true

        return current.state != previous.state
    }

    fun stateTime(instance: Instance): Long = stateWatches[instance]?.time ?: -1L
}
