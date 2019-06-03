package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.Behaviors
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.action.AbstractAction
import com.cognifide.gradle.aem.common.instance.names
import kotlinx.coroutines.isActive
import org.apache.commons.lang3.time.StopWatch

open class CheckAction(aem: AemExtension) : AbstractAction(aem) {

    var delay = aem.props.long("instance.check.delay") ?: 1000

    var resume = aem.props.flag("instance.check.resume")

    var checks: CheckGroup.() -> List<Check> = { throw InstanceException("No instance checks defined!") }

    private val stopWatch = StopWatch()

    val running: Long
        get() = stopWatch.time

    var error: Exception? = null

    val aborted: Boolean
        get() = error != null

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to check.")
            return
        }

        check()
    }

    private fun check() {
        aem.logger.info("Checking instance(s): ${instances.names}")

        aem.progressIndicator {
            val instanceChecks = mutableMapOf<Instance, CheckGroup>()

            updater = {
                val instanceCheckSummaries = instanceChecks.toSortedMap(compareBy { it.name })
                        .map { (instance, checks) -> "${instance.name}: ${checks.summary.decapitalize()}" }
                update(instanceCheckSummaries.joinToString(" | "))
            }

            stopWatch.start()
            aem.parallel.each(instances) { instance ->
                while (isActive) {
                    val checks = CheckGroup(this@CheckAction, instance, checks)
                    checks.check()
                    instanceChecks[instance] = checks

                    if (aborted || checks.done) {
                        break
                    }

                    Behaviors.waitFor(delay)
                }
            }
            stopWatch.stop()

            if (aborted && !resume) {
                instanceChecks.forEach { (_, group) ->
                    group.statusLogger.entries.forEach { aem.logger.log(it.level, it.details) }
                }

                error?.let { throw it }
            }
        }
    }
}