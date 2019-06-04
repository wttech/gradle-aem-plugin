package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.check.*
import com.cognifide.gradle.aem.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for unavailable local instances.
 */
class AwaitDownAction(aem: AemExtension) : LocalInstanceAction(aem) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        stateTimeout = aem.props.long("instance.awaitDown.stateTimeout")
                ?: TimeUnit.MINUTES.toMillis(1)
        constantTimeout = aem.props.long("instance.awaitDown.constantTimeout")
                ?: TimeUnit.MINUTES.toMillis(5)
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var unavailableOptions: UnavailableCheck.() -> Unit = {
        controlPortAge = aem.props.long("instance.awaitDown.controlPortAge")
                ?: TimeUnit.MINUTES.toMillis(3)
    }

    fun unavailable(options: UnavailableCheck.() -> Unit) {
        unavailableOptions = options
    }

    val runner = CheckRunner(aem).apply {
        delay = aem.props.long("instance.awaitDown.delay") ?: TimeUnit.SECONDS.toMillis(1)
        retries = aem.props.int("instance.awaitDown.retries") ?: 1
        verbose = aem.props.boolean("instance.awaitDown.verbose") ?: true

        checks {
            listOf(
                    timeout(timeoutOptions),
                    unavailable(unavailableOptions)
            )
        }
    }

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            aem.logger.info("No instances to await down.")
            return
        }

        aem.logger.info("Awaiting instance(s) down: ${instances.names}")

        runner.check(instances)
    }
}