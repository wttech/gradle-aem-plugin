package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.check.BundlesCheck
import com.cognifide.gradle.aem.common.instance.action.check.ComponentsCheck
import com.cognifide.gradle.aem.common.instance.action.check.EventsCheck
import com.cognifide.gradle.aem.common.instance.action.check.TimeoutCheck

class AwaitAction(aem: AemExtension) : CheckAction(aem) {

    init {
        checks = { instance ->
            setOf(
                    TimeoutCheck(this, instance),
                    BundlesCheck(this, instance),
                    EventsCheck(this, instance),
                    ComponentsCheck(this, instance)
            )
        }
    }
}