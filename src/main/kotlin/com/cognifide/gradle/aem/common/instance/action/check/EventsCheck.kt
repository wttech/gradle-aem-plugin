package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.CheckAction

@Suppress("MagicNumber")
class EventsCheck(action: CheckAction, instance: Instance) : DefaultCheck(action, instance) {

    init {
        sync.apply {
            http.connectionTimeout = 250
            http.connectionRetries = false
        }
    }

    var unstableTopics = listOf(
            "org/osgi/framework/ServiceEvent/*",
            "org/osgi/framework/FrameworkEvent/*",
            "org/osgi/framework/BundleEvent/*"
    )

    var unstableAgeMillis = 5000L

    override fun check() {
        val state = sync.osgiFramework.determineEventState()

        if (state.unknown) {
            statusLogger.error("Unknown event state on $instance")
            return
        }

        val unstable = state.matching(unstableTopics, unstableAgeMillis, instance.zoneId)
        if (unstable.isNotEmpty()) {
            statusLogger.error("Events causing instability detected on $instance:${unstable.joinToString("\n")}")
        }
    }
}