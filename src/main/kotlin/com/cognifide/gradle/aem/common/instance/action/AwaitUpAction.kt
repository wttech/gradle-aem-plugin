package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.check.*
import com.cognifide.gradle.aem.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for stable condition of all instances of any type.
 */
class AwaitUpAction(aem: AemExtension) : DefaultAction(aem) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        unavailableTime.apply {
            convention(TimeUnit.MINUTES.toMillis(1))
            aem.prop.long("instance.awaitUp.timeout.unavailableTime")?.let { set(it) }
        }
        stateTime.apply {
            convention(TimeUnit.MINUTES.toMillis(10))
            aem.prop.long("instance.awaitUp.timeout.stateTime")?.let { set(it) }
        }
        constantTime.apply {
            convention(TimeUnit.MINUTES.toMillis(30))
            aem.prop.long("instance.awaitUp.timeout.constantTime")?.let { set(it) }
        }
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var helpOptions: HelpCheck.() -> Unit = {
        enabled.apply {
            aem.prop.boolean(("instance.awaitUp.help.enabled"))?.let { set(it) }
        }
        stateTime.apply {
            aem.prop.long("instance.awaitUp.help.stateTime")?.let { set(it) }
        }
        bundleStartStates.apply {
            aem.prop.list("instance.awaitUp.help.bundleStartStates")?.let { set(it) }
        }
        bundleStartRetry.apply {
            aem.prop.long("instance.awaitUp.help.bundleStartRetry")?.let { afterSquaredSecond(it) }
        }
        bundleStartDelay.apply {
            aem.prop.long("instance.awaitUp.help.bundleStartDelay")?.let { set(it) }
        }
    }

    fun help(options: HelpCheck.() -> Unit) {
        helpOptions = options
    }

    private var installerOptions: InstallerCheck.() -> Unit = {
        busy.apply {
            aem.prop.boolean("instance.awaitUp.installer.busy.enabled")?.let { set(it) }
        }
        pause.apply {
            aem.prop.boolean("instance.awaitUp.installer.pause.enabled")?.let { set(it) }
        }
    }

    fun installer(options: InstallerCheck.() -> Unit) {
        installerOptions = options
    }

    private var bundlesOptions: BundlesCheck.() -> Unit = {
        symbolicNamesIgnored.apply {
            aem.prop.list("instance.awaitUp.bundles.symbolicNamesIgnored")?.let { set(it) }
        }
    }

    fun bundles(options: BundlesCheck.() -> Unit) {
        bundlesOptions = options
    }

    private var eventsOptions: EventsCheck.() -> Unit = {
        unstableTopics.apply {
            aem.prop.list("instance.awaitUp.event.unstableTopics")?.let { set(it) }
        }
        unstableAgeMillis.apply {
            convention(TimeUnit.SECONDS.toMillis(5))
            aem.prop.long("instance.awaitUp.event.unstableAgeMillis")?.let { set(it) }
        }
        ignoredDetails.apply {
            aem.prop.list("instance.awaitUp.event.ignoredDetails")?.let { set(it) }
        }
    }

    fun events(options: EventsCheck.() -> Unit) {
        eventsOptions = options
    }

    private var componentsOptions: ComponentsCheck.() -> Unit = {
        platformComponents.apply {
            aem.prop.list("instance.awaitUp.components.platform")?.let { set(it) }
        }
        specificComponents.apply {
            convention(
                aem.obj.provider {
                    aem.javaPackages.map { "$it.*" }
                }
            )
            aem.prop.list("instance.awaitUp.components.specific")?.let { set(it) }
        }
    }

    fun components(options: ComponentsCheck.() -> Unit) {
        componentsOptions = options
    }

    private var unchangedOptions: UnchangedCheck.() -> Unit = {
        awaitTime.apply {
            convention(TimeUnit.SECONDS.toMillis(3))
            aem.prop.long("instance.awaitUp.unchanged.awaitTime")?.let { set(it) }
        }
    }

    fun unchanged(options: UnchangedCheck.() -> Unit) {
        unchangedOptions = options
    }

    fun runner(options: CheckRunner.() -> Unit) {
        runner.apply(options)
    }

    private val runner = CheckRunner(aem).apply {
        delay.apply {
            convention(TimeUnit.SECONDS.toMillis(3))
            aem.prop.long("instance.awaitUp.delay")?.let { set(it) }
        }
        doneTimes.apply {
            convention(5)
            aem.prop.long("instance.awaitUp.doneTimes")?.let { set(it) }
        }
        verbose.apply {
            aem.prop.boolean("instance.awaitUp.verbose")?.let { set(it) }
        }
        logInstantly.apply {
            aem.prop.boolean("instance.awaitUp.logInstantly")?.let { set(it) }
        }

        checks {
            listOf(
                timeout(timeoutOptions),
                help(helpOptions),
                bundles(bundlesOptions),
                events(eventsOptions),
                installer(installerOptions),
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

    companion object {
        /**
         * Simplified options to more quickly check condition of AEM instances
         * assuming that no package deployment was performed before running checks.
         */
        fun noPackageDeployOptions(): AwaitUpAction.() -> Unit = {
            runner { doneTimes.set(1) }
            unchanged { enabled.set(false) }
        }
    }
}
