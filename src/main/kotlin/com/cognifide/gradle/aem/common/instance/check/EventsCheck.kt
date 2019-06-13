package com.cognifide.gradle.aem.common.instance.check

import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.StringUtils

@Suppress("MagicNumber")
class EventsCheck(group: CheckGroup) : DefaultCheck(group) {

    var unstableTopics = listOf<String>()

    var unstableAgeMillis = TimeUnit.SECONDS.toMillis(5)

    init {
        sync.apply {
            http.connectionTimeout = 250
            http.connectionRetries = false
        }
    }

    override fun check() {
        aem.logger.info("Checking OSGi events on $instance")

        val state = state(sync.osgiFramework.determineEventState())

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
                        1 -> "Event unstable '${StringUtils.abbreviate(unstable.first().details, EVENT_DETAILS_LENGTH)}'"
                        else -> "Events unstable (${unstable.size})"
                    },
                    "Events causing instability (${unstable.size}) detected on $instance:\n${logValues(unstable)}"
            )
        }
    }

    companion object {
        const val EVENT_DETAILS_LENGTH = 64
    }
}