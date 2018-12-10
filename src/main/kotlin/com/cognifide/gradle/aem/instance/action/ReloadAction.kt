package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.ProgressCountdown
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceException
import java.util.concurrent.TimeUnit

/**
 * Reloads all instances and waits until all be stable.
 */
class ReloadAction(aem: AemExtension) : AwaitAction(aem) {

    /**
     * Time in milliseconds to postpone await action after triggering instances restart.
     */
    var awaitDelay: Long = aem.props.long("aem.reload.awaitDelay") ?: TimeUnit.SECONDS.toMillis(10)

    private fun reload() {
        val reloaded = mutableListOf<Instance>()

        aem.parallelWith(instances) {
            try {
                sync.restartFramework()
                reloaded += this
            } catch (e: InstanceException) { // still await timeout will fail
                aem.logger.error("Instance is unavailable: $this", e)
            }
        }

        if (reloaded.isNotEmpty()) {
            val unavailable = instances - reloaded
            val countdown = ProgressCountdown(aem.project, awaitDelay)

            aem.logger.lifecycle("Reloading instance(s): ${reloaded.size} triggered, ${unavailable.size} unavailable")
            countdown.run()
        } else {
            throw InstanceException("All instances are unavailable.")
        }
    }

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to reload.")
            return
        }

        reload()
        super.perform()
    }
}