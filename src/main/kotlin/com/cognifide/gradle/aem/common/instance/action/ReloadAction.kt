package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.build.ProgressCountdown
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException
import java.util.concurrent.TimeUnit

/**
 * Reloads all instances.
 */
class ReloadAction(aem: AemExtension) : AnyInstanceAction(aem) {

    /**
     * Time in milliseconds to postpone awaitUp action after triggering instances restart.
     */
    var delay: Long = aem.props.long("instance.reload.delay") ?: TimeUnit.SECONDS.toMillis(10)

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to reload.")
            return
        }

        reload()
    }

    private fun reload() {
        val reloaded = mutableListOf<Instance>()

        aem.parallel.with(instances) {
            try {
                sync.osgiFramework.restart()
                reloaded += this
            } catch (e: InstanceException) { // still await up timeout will fail
                aem.logger.error("Instance is unavailable: $this")
                aem.logger.info("Error details", e)
            }
        }

        if (reloaded.isNotEmpty()) {
            val unavailable = instances - reloaded
            val countdown = ProgressCountdown(aem.project, delay)

            aem.logger.lifecycle("Reloading instance(s): ${reloaded.size} triggered, ${unavailable.size} unavailable")
            countdown.run()
        } else {
            throw InstanceException("All instances are unavailable.")
        }
    }
}