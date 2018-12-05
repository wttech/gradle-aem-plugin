package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.ProgressCountdown
import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceException
import java.util.concurrent.TimeUnit
import org.gradle.api.tasks.Input

/**
 * Reloads all instances and waits until all be stable.
 */
class ReloadAction(aem: AemExtension) : AwaitAction(aem) {

    /**
     * Time in milliseconds to postpone instance stability checks after triggering instances restart.
     */
    @Input
    var delay: Long = aem.props.long("aem.reload.delay") ?: TimeUnit.SECONDS.toMillis(10)

    private fun reload() {
        val reloaded = mutableListOf<Instance>()

        aem.parallelWith(instances) {
            try {
                sync.reload()
                reloaded += this
            } catch (e: InstanceException) { // still await timeout will fail
                aem.logger.error("Instance is unavailable: $this", e)
            }
        }

        if (reloaded.isNotEmpty()) {
            val unavailable = instances - reloaded
            val header = "Reloading instance(s): ${reloaded.size} triggered, ${unavailable.size} unavailable"

            ProgressCountdown(aem.project, header, delay).run()
        } else {
            throw InstanceException("All instances are unavailable.")
        }
    }

    override fun perform() {
        if (instances.isEmpty()) {
            aem.logger.info("No instances to reload.")
            return
        }

        reload()
        super.perform()
    }
}