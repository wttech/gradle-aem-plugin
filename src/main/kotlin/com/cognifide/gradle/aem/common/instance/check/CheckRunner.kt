package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import kotlinx.coroutines.isActive
import org.apache.commons.lang3.time.StopWatch

class CheckRunner(internal val aem: AemExtension) {

    var delay = 0L

    var resume = false

    var checks: CheckGroup.() -> List<Check> = { throw InstanceException("No instance checks defined!") }

    private val runningWatch = StopWatch()

    val runningTime: Long
        get() = runningWatch.time

    var abortCause: Exception? = null

    val aborted: Boolean
        get() = abortCause != null

    fun check(instances: Iterable<Instance>) {
        aem.progressIndicator {
            val instanceChecks = mutableMapOf<Instance, CheckGroup>()

            updater = {
                val instanceCheckSummaries = instanceChecks.toSortedMap(compareBy { it.name })
                        .map { (instance, checks) -> "${instance.name}: ${checks.summary.decapitalize()}" }
                update(instanceCheckSummaries.joinToString(" | "))
            }

            runningWatch.start()
            aem.parallel.each(instances) { instance ->
                while (isActive) {
                    val checks = CheckGroup(this@CheckRunner, instance, checks)
                    checks.check()
                    instanceChecks[instance] = checks

                    if (aborted || checks.done) {
                        break
                    }

                    Behaviors.waitFor(delay)
                }
            }
            runningWatch.stop()

            if (aborted && !resume) {
                instanceChecks.forEach { (_, group) ->
                    group.statusLogger.entries.forEach { aem.logger.log(it.level, it.details) }
                }

                abortCause?.let { throw it }
            }
        }
    }
}