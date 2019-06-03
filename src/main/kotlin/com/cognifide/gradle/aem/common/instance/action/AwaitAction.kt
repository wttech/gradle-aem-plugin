package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.check.*

class AwaitAction(aem: AemExtension) : CheckAction(aem) {

    private var timeoutOptions: TimeoutCheck.() -> Unit = {}

    fun timeout(options: TimeoutCheck.() -> Unit) {
        timeoutOptions = options
    }

    private var bundlesOptions: BundlesCheck.() -> Unit = {}

    fun bundles(options: BundlesCheck.() -> Unit) {
        bundlesOptions = options
    }

    private var eventsOptions: EventsCheck.() -> Unit = {}

    fun events(options: EventsCheck.() -> Unit) {
        eventsOptions = options
    }

    private var componentsOptions: ComponentsCheck.() -> Unit = {}

    fun components(options: ComponentsCheck.() -> Unit) {
        componentsOptions = options
    }

    init {
        checks = {
            listOf(
                    TimeoutCheck(this).apply(timeoutOptions),
                    BundlesCheck(this).apply(bundlesOptions),
                    EventsCheck(this).apply(eventsOptions),
                    ComponentsCheck(this).apply(componentsOptions)
            )
        }
    }
}