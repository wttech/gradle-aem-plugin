package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.check.*
import com.cognifide.gradle.aem.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for unavailable local instances.
 */
class AwaitDownAction(aem: AemExtension) : DefaultAction(aem) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        stateTime.apply {
            convention(TimeUnit.MINUTES.toMillis(2))
            aem.prop.long("instance.awaitDown.timeout.stateTime")?.let { set(it) }
        }
        constantTime.apply {
            convention(TimeUnit.MINUTES.toMillis(10))
            aem.prop.long("instance.awaitDown.timeout.constantTime")?.let { set(it) }
        }
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var unavailableOptions: UnavailableCheck.() -> Unit = {
        utilisationTime.apply {
            convention(TimeUnit.SECONDS.toMillis(10))
            aem.prop.long("instance.awaitDown.unavailable.utilizationTime")?.let { set(it) }
        }
    }

    fun unavailable(options: UnavailableCheck.() -> Unit) {
        unavailableOptions = options
    }

    private var unchangedOptions: UnchangedCheck.() -> Unit = {
        awaitTime.apply {
            convention(TimeUnit.SECONDS.toMillis(3))
            aem.prop.long("instance.awaitDown.unchanged.awaitTime")?.let { set(it) }
        }
    }

    fun unchanged(options: UnchangedCheck.() -> Unit) {
        unchangedOptions = options
    }

    private val runner = CheckRunner(aem).apply {
        delay.apply {
            convention(TimeUnit.SECONDS.toMillis(2))
            aem.prop.long("instance.awaitDown.delay")?.let { set(it) }
        }
        verbose.apply {
            convention(true)
            aem.prop.boolean("instance.awaitDown.verbose")?.let { set(it) }
        }

        checks {
            listOf(
                    timeout(timeoutOptions),
                    unavailable(unavailableOptions),
                    unchanged(unchangedOptions)
            )
        }
    }

    override fun perform(instances: Collection<Instance>) {
        if (instances.isEmpty()) {
            logger.info("No instances to await down.")
            return
        }

        logger.info("Awaiting instance(s) down: ${instances.names}")

        runner.check(instances)
    }
}
