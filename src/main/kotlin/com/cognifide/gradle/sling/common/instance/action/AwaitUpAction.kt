package com.cognifide.gradle.sling.common.instance.action

import com.cognifide.gradle.sling.SlingExtension
import com.cognifide.gradle.sling.common.instance.Instance
import com.cognifide.gradle.sling.common.instance.check.*
import com.cognifide.gradle.sling.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for stable condition of all instances of any type.
 */
class AwaitUpAction(sling: SlingExtension) : DefaultAction(sling) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        unavailableTime.apply {
            convention(TimeUnit.MINUTES.toMillis(1))
            sling.prop.long("instance.awaitUp.timeout.unavailableTime")?.let { set(it) }
        }
        stateTime.apply {
            convention(TimeUnit.MINUTES.toMillis(10))
            sling.prop.long("instance.awaitUp.timeout.stateTime")?.let { set(it) }
        }
        constantTime.apply {
            convention(TimeUnit.MINUTES.toMillis(30))
            sling.prop.long("instance.awaitUp.timeout.constantTime")?.let { set(it) }
        }
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var helpOptions: HelpCheck.() -> Unit = {
        enabled.apply {
            sling.prop.boolean(("instance.awaitUp.help.enabled"))?.let { set(it) }
        }
        stateTime.apply {
            sling.prop.long("instance.awaitUp.help.stateTime")?.let { set(it) }
        }
        bundleStartStates.apply {
            sling.prop.list("instance.awaitUp.help.bundleStartStates")?.let { set(it) }
        }
        bundleStartRetry.apply {
            sling.prop.long("instance.awaitUp.help.bundleStartRetry")?.let { afterSquaredSecond(it) }
        }
        bundleStartDelay.apply {
            sling.prop.long("instance.awaitUp.help.bundleStartDelay")?.let { set(it) }
        }
    }

    fun help(options: HelpCheck.() -> Unit) {
        helpOptions = options
    }

    private var bundlesOptions: BundlesCheck.() -> Unit = {
        symbolicNamesIgnored.apply {
            sling.prop.list("instance.awaitUp.bundles.symbolicNamesIgnored")?.let { set(it) }
        }
    }

    fun bundles(options: BundlesCheck.() -> Unit) {
        bundlesOptions = options
    }

    private var eventsOptions: EventsCheck.() -> Unit = {
        unstableTopics.apply {
            sling.prop.list("instance.awaitUp.event.unstableTopics")?.let { set(it) }
        }
        unstableAgeMillis.apply {
            convention(TimeUnit.SECONDS.toMillis(5))
            sling.prop.long("instance.awaitUp.event.unstableAgeMillis")?.let { set(it) }
        }
        ignoredDetails.apply {
            sling.prop.list("instance.awaitUp.event.ignoredDetails")?.let { set(it) }
        }
    }

    fun events(options: EventsCheck.() -> Unit) {
        eventsOptions = options
    }

    private var componentsOptions: ComponentsCheck.() -> Unit = {
        platformComponents.apply {
            sling.prop.list("instance.awaitUp.components.platform")?.let { set(it) }
        }
        specificComponents.apply {
            convention(sling.obj.provider { sling.javaPackages.map { "$it.*" } })
            sling.prop.list("instance.awaitUp.components.specific")?.let { set(it) }
        }
    }

    fun components(options: ComponentsCheck.() -> Unit) {
        componentsOptions = options
    }

    private var unchangedOptions: UnchangedCheck.() -> Unit = {
        awaitTime.apply {
            convention(TimeUnit.SECONDS.toMillis(3))
            sling.prop.long("instance.awaitUp.unchanged.awaitTime")?.let { set(it) }
        }
    }

    fun unchanged(options: UnchangedCheck.() -> Unit) {
        unchangedOptions = options
    }

    private val runner = CheckRunner(sling).apply {
        delay.apply {
            convention(TimeUnit.SECONDS.toMillis(1))
            sling.prop.long("instance.awaitUp.delay")?.let { set(it) }
        }
        verbose.apply {
            sling.prop.boolean("instance.awaitUp.verbose")?.let { set(it) }
        }
        logInstantly.apply {
            sling.prop.boolean("instance.awaitUp.logInstantly")?.let { set(it) }
        }

        checks {
            listOf(
                    timeout(timeoutOptions),
                    help(helpOptions),
                    bundles(bundlesOptions),
                    events(eventsOptions),
                    components(componentsOptions),
                    unchanged(unchangedOptions)
            )
        }
    }

    override fun perform(instances: Collection<Instance>) {
        if (instances.isEmpty()) {
            logger.info("No instances to await up.")
            return
        }

        logger.info("Awaiting instance(s) up: ${instances.names}")

        runner.check(instances)
    }
}
