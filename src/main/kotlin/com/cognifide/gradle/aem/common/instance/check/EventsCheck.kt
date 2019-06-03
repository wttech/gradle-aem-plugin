package com.cognifide.gradle.aem.common.instance.check

@Suppress("MagicNumber")
class EventsCheck(group: CheckGroup) : DefaultCheck(group) {

    var unstableTopics = aem.props.list("instance.check.event.unstableTopics") ?: listOf(
            "org/osgi/framework/ServiceEvent/*",
            "org/osgi/framework/FrameworkEvent/*",
            "org/osgi/framework/BundleEvent/*"
    )

    var unstableAgeMillis = aem.props.long("instance.check.event.unstableAgeMillis") ?: 5000L

    init {
        sync.apply {
            http.connectionTimeout = 250
            http.connectionRetries = false
        }
    }

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
                    when (unstable.size) {
                        1 -> "Event unstable '${unstable.first().topic}'"
                        else -> "Events unstable (${unstable.size})"
                    },
                    "Events causing instability detected on $instance:${unstable.joinToString("\n")}"
            )
        }
    }
}