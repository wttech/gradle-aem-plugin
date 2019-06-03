package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.check.*
import com.cognifide.gradle.aem.common.instance.names
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang3.time.StopWatch

open class CheckAction(aem: AemExtension) : AbstractAction(aem) {

    var checks: (Instance) -> Set<Check> = { setOf() }

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
            val instanceStatuses = mutableMapOf<Instance, String>()

            updater = {
                update(instanceStatuses.map { (instance, status) ->
                    "${instance.name} $status"
                }.joinToString(" | "))
            }

            runBlocking {
                stopWatch.start()
                aem.parallel.with(instances) {
                    while (isActive) {
                        val checks = Checks(this@CheckAction, this, checks(this))

                        checks.check()
                        if (aborted || checks.done) {
                            break
                        }

                        instanceStatuses[this] = checks.status
                    }
                }
                stopWatch.stop()
            }

            if (aborted) {
                error?.let { throw it }
            }
        }
    }
}