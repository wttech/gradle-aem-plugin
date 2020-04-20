package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.utils.shortenClass
import java.util.concurrent.TimeUnit
import org.apache.commons.lang3.StringUtils

@Suppress("MagicNumber")
class EventsCheck(group: CheckGroup) : DefaultCheck(group) {

    val unstableTopics = aem.obj.strings {
        convention(listOf(
                "org/osgi/framework/ServiceEvent/*",
                "org/osgi/framework/FrameworkEvent/*",
                "org/osgi/framework/BundleEvent/*"
        ))
    }

    val unstableAgeMillis = aem.obj.long { convention(TimeUnit.SECONDS.toMillis(5)) }

    init {
        sync.apply {
            http.connectionTimeout.convention(250)
            http.connectionRetries.convention(false)
        }
    }

    override fun check() {
        logger.info("Checking OSGi events on $instance")

        val state = state(sync.osgiFramework.determineEventState())

        if (state.unknown) {
            statusLogger.error(
                    "Events unknown",
                    "Unknown event state on $instance"
            )
            return
        }

        val unstable = state.matching(unstableTopics.get(), unstableAgeMillis.get(), instance.zoneId)
        if (unstable.isNotEmpty()) {
            statusLogger.error(
                    when (unstable.size) {
                        1 -> "Event unstable '${StringUtils.abbreviate(unstable.first().details.shortenClass(), EVENT_DETAILS_LENGTH)}'"
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
