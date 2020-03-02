package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.InstanceException

/**
 * Reloads all instances (both remote and local instances).
 */
class ReloadAction(aem: AemExtension) : DefaultAction(aem) {

    override fun perform(instances: Collection<Instance>) {
        if (instances.isEmpty()) {
            aem.logger.info("No instances to reload.")
            return
        }

        reload(instances)
    }

    private fun reload(instances: Collection<Instance>) {
        val reloaded = mutableListOf<Instance>()

        common.parallel.with(instances) {
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

            aem.logger.info("Reloading instance(s): ${reloaded.size} triggered, ${unavailable.size} unavailable")
        } else {
            throw InstanceException("All instances are unavailable.")
        }
    }
}
