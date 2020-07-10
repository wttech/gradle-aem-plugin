package com.cognifide.gradle.sling.common.instance.action

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.InstanceException
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Reloads all instances (both remote and local instances).
 */
class ReloadAction(sling: SlingExtension) : DefaultAction(sling) {

    override fun perform(instances: Collection<Instance>) {
        if (instances.isEmpty()) {
            sling.logger.info("No instances to reload.")
            return
        }

        reload(instances)
    }

    private fun reload(instances: Collection<Instance>) {
        val reloaded = CopyOnWriteArrayList<Instance>()

        common.parallel.with(instances) {
            try {
                sync.osgiFramework.restart()
                reloaded += this
            } catch (e: InstanceException) { // still await up timeout will fail
                sling.logger.error("Instance is unavailable: $this")
                sling.logger.info("Error details", e)
            }
        }

        if (reloaded.isNotEmpty()) {
            val unavailable = instances - reloaded

            sling.logger.info("Reloading instance(s): ${reloaded.size} triggered, ${unavailable.size} unavailable")
        } else {
            throw InstanceException("All instances are unavailable.")
        }
    }
}
