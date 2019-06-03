package com.cognifide.gradle.aem.common.instance.action

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.action.check.BundlesCheck
import com.cognifide.gradle.aem.common.instance.action.check.ComponentsCheck
import com.cognifide.gradle.aem.common.instance.action.check.EventsCheck
import com.cognifide.gradle.aem.common.instance.action.check.TimeoutCheck

class AwaitAction(aem: AemExtension) : CheckAction(aem) {

    init {
        checks = {
            setOf(
                    TimeoutCheck(this),
                    BundlesCheck(this),
                    EventsCheck(this),
                    ComponentsCheck(this)
            )
        }
    }
}