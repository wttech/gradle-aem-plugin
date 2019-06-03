package com.cognifide.gradle.aem.common.instance.action.check

@Suppress("MagicNumber")
class EventsCheck(group: CheckGroup) : DefaultCheck(group) {

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
        aem.logger.info("Checking OSGi events on $instance")

        val state = sync.osgiFramework.determineEventState()

        if (state.unknown) {
            statusLogger.error(
                    "Events unknown",
                    "Unknown event state on $instance"
            )
            return
        }

        val unstable = state.matching(unstableTopics, unstableAgeMillis, instance.zoneId)
        if (unstable.isNotEmpty()) {
            statusLogger.error(
                    "Events unstable (${unstable.size})",
                    "Events causing instability detected on $instance:${unstable.joinToString("\n")}"
            )
        }
    }
}