package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.check.*
import com.cognifide.gradle.aem.common.instance.local.Status
import com.cognifide.gradle.aem.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for unavailable local instances.
 */
class AwaitDownAction(aem: AemExtension) : LocalInstanceAction(aem) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        state = aem.props.long("instance.awaitDown.timeout.state")
                ?: TimeUnit.MINUTES.toMillis(2)
        constant = aem.props.long("instance.awaitDown.timeout.constant")
                ?: TimeUnit.MINUTES.toMillis(10)
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var unavailableOptions: UnavailableCheck.() -> Unit = {
        statusExpected = Status.of(aem.props.string("instance.awaitDown.unavailable.statusExpected") ?: Status.UNKNOWN.name)
        utilisationTime = aem.props.long("instance.awaitDown.unavailable.utilizationTime")
                ?: TimeUnit.SECONDS.toMillis(10)
    }

    fun unavailable(options: UnavailableCheck.() -> Unit) {
        unavailableOptions = options
    }

    private val runner = CheckRunner(aem).apply {
        wait = aem.props.long("instance.awaitDown.wait") ?: TimeUnit.SECONDS.toMillis(5)
        delay = aem.props.long("instance.awaitDown.delay") ?: TimeUnit.SECONDS.toMillis(1)
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