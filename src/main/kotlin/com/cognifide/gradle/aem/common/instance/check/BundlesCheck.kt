package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.utils.shortenClass

@Suppress("MagicNumber")
class BundlesCheck(group: CheckGroup) : DefaultCheck(group) {

    val symbolicNamesIgnored = aem.obj.strings { convention(listOf()) }

    init {
        sync.apply {
            http.connectionTimeout.convention(1_500)
            http.connectionRetries.convention(false)
        }
    }

    override fun check() {
        logger.info("Checking OSGi bundles on $instance")

        val state = state(sync.osgiFramework.determineBundleState())

        if (state.unknown) {
            statusLogger.error(
                "Bundles unknown",
                "Unknown bundle state on $instance"
            )
            return
        }

        val unstable = state.bundlesExcept(symbolicNamesIgnored.get()).filter { !it.stable }
        if (unstable.isNotEmpty()) {
            statusLogger.error(
                when (unstable.size) {
                    1 -> "Bundle unstable '${unstable.first().symbolicName.shortenClass()}'"
                    in 2..10 -> "Bundles unstable (${unstable.size})"
                    else -> "Bundles stable (${state.stablePercent})"
                },
                "Unstable bundles detected (${unstable.size}) on $instance:\n${logValues(unstable)}"
            )
        }
    }
}
