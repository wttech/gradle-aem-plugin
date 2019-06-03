package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.action.check.Check
import com.cognifide.gradle.aem.common.instance.action.check.CheckGroup
import com.cognifide.gradle.aem.common.instance.names
import kotlinx.coroutines.isActive
import org.apache.commons.lang3.time.StopWatch

open class CheckAction(aem: AemExtension) : AbstractAction(aem) {

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
            val allChecks = mutableMapOf<Instance, CheckGroup>()

            updater = {
                update(allChecks.map { (instance, group) ->
                    "${instance.name}: ${group.summary.decapitalize()}"
                }.joinToString(" | "))
            }

            stopWatch.start()
            aem.parallel.each(instances) { instance ->
                while (isActive) {
                    val checks = CheckGroup(this@CheckAction, instance, checks)
                    checks.check()
                    allChecks[instance] = checks

                    if (aborted || checks.done) {
                        break
                    }
                }
            }
            stopWatch.stop()

            if (aborted) {
                allChecks.forEach { (_, group) ->
                    group.statusLogger.entries.forEach { aem.logger.log(it.level, it.details) }
                }

                error?.let { throw it }
            }
        }
    }
}