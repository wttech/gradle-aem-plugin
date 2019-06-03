package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.check.BundlesCheck
import com.cognifide.gradle.aem.common.instance.action.check.ComponentsCheck
import com.cognifide.gradle.aem.common.instance.action.check.EventsCheck
import com.cognifide.gradle.aem.common.instance.action.check.TimeoutCheck

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
                    TimeoutCheck(this),
                    BundlesCheck(this),
                    EventsCheck(this),
                    ComponentsCheck(this)
            )
        }
    }
}