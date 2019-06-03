package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.action.check.*
import com.cognifide.gradle.aem.common.instance.names
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.time.StopWatch

open class CheckAction(aem: AemExtension) : AbstractAction(aem) {

    var checks: CheckGroup.() -> Set<Check> = { throw InstanceException("No instance checks defined!") }

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
                    "${instance.name}|${group.summary.decapitalize()}"
                }.joinToString(" "))
            }

            runBlocking {
                stopWatch.start()
                aem.parallel.with(instances) {
                    while (isActive) {
                        val checks = CheckGroup(this@CheckAction, this, checks)

                        checks.check()
                        if (aborted || checks.done) {
                            break
                        }

                        allChecks[this] = checks
                    }
                }
                stopWatch.stop()
            }

            if (aborted) {
                allChecks.forEach { (_, group) ->
                    group.statusLogger.entries.forEach { aem.logger.log(it.level, it.details) }
                }

                error?.let { throw it }
            }
        }
    }
}