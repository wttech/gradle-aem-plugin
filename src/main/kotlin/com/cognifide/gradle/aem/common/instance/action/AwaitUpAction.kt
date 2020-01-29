package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.check.*
import com.cognifide.gradle.aem.common.instance.names
import java.util.concurrent.TimeUnit

/**
 * Awaits for stable condition of all instances of any type.
 */
class AwaitUpAction(aem: AemExtension) : AnyInstanceAction(aem) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {
        stateTime = aem.prop.long("instance.awaitUp.timeout.stateTime")
                ?: TimeUnit.MINUTES.toMillis(10)
        constantTime = aem.prop.long("instance.awaitUp.timeout.constantTime")
                ?: TimeUnit.MINUTES.toMillis(30)
    }

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var bundlesOptions: BundlesCheck.() -> Unit = {
        symbolicNamesIgnored = aem.prop.list("instance.awaitUp.bundles.symbolicNamesIgnored")
                ?: listOf()
    }

    fun bundles(options: BundlesCheck.() -> Unit) {
        bundlesOptions = options
    }

    private var eventsOptions: EventsCheck.() -> Unit = {
        unstableTopics = aem.prop.list("instance.awaitUp.event.unstableTopics") ?: listOf(
                "org/osgi/framework/ServiceEvent/*",
                "org/osgi/framework/FrameworkEvent/*",
                "org/osgi/framework/BundleEvent/*"
        )
        unstableAgeMillis = aem.prop.long("instance.awaitUp.event.unstableAgeMillis")
                ?: TimeUnit.SECONDS.toMillis(5)
    }

    fun events(options: EventsCheck.() -> Unit) {
        eventsOptions = options
    }

    private var componentsOptions: ComponentsCheck.() -> Unit = {
        platformComponents = aem.prop.list("instance.awaitUp.components.platform")
                ?: listOf("com.day.crx.packaging.*", "org.apache.sling.installer.*")
        specificComponents = aem.prop.list("instance.awaitUp.components.specific")
                ?: aem.javaPackages.map { "$it.*" }
    }

    fun components(options: ComponentsCheck.() -> Unit) {
        componentsOptions = options
    }

    private var unchangedOptions: UnchangedCheck.() -> Unit = {
        awaitTime = aem.prop.long("instance.awaitUp.unchanged.awaitTime")
                ?: TimeUnit.SECONDS.toMillis(3)
    }

    fun unchanged(options: UnchangedCheck.() -> Unit) {
        unchangedOptions = options
    }

    private val runner = CheckRunner(aem).apply {
        delay = aem.prop.long("instance.awaitUp.delay") ?: TimeUnit.SECONDS.toMillis(1)
        verbose = aem.prop.boolean("instance.awaitUp.verbose") ?: true

        checks {
            listOf(
                    timeout(timeoutOptions),
                    bundles(bundlesOptions),
                    events(eventsOptions),
                    components(componentsOptions),
                    unchanged(unchangedOptions)
            )
        }
    }

    override fun perform() {
        if (!enabled) {
            return
        }

        if (instances.isEmpty()) {
            logger.info("No instances to await up.")
            return
        }

        logger.info("Awaiting instance(s) up: ${instances.names}")

        runner.check(instances)
    }
}
