package com.cognifide.gradle.aem.instance.action

import com.cognifide.gradle.aem.instance.Instance
import com.cognifide.gradle.aem.instance.InstanceException
import com.cognifide.gradle.aem.instance.InstanceSync
import com.cognifide.gradle.aem.internal.ProgressCountdown
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import java.util.concurrent.TimeUnit

/**
 * Reloads all instances and waits until all be stable.
 */
class ReloadAction(project: Project) : AwaitAction(project) {

    /**
     * Time in milliseconds to postpone instance stability checks after triggering instances restart.
     */
    @Input
    var delay: Long = aem.props.long("aem.reload.delay", TimeUnit.SECONDS.toMillis(10))

    private fun reload() {
        val reloaded = mutableListOf<Instance>()
        instances.parallelStream().forEach { instance ->
            try {
                InstanceSync(project, instance).reload()
                reloaded += instance
            } catch (e: InstanceException) { // still await timeout will fail
                logger.error("Instance is unavailable: $instance", e)
            }
        }

        if (reloaded.isNotEmpty()) {
            val unavailable = instances - reloaded
            val header = "Reloading instance(s): ${reloaded.size} triggered, ${unavailable.size} unavailable"

            ProgressCountdown(project, header, delay).run()
        } else {
            throw InstanceException("All instances are unavailable.")
        }
    }

    override fun perform() {
        if (instances.isEmpty()) {
            logger.info("No instances to reload.")
            return
        }

        reload()
        super.perform()
    }

}