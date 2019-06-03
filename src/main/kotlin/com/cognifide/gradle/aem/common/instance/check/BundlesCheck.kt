package com.cognifide.gradle.aem.common.instance.check

import com.cognifide.gradle.aem.common.utils.Formats

@Suppress("MagicNumber")
class BundlesCheck(group: CheckGroup) : DefaultCheck(group) {

    var symbolicNamesIgnored = aem.props.list("instance.check.bundles.symbolicNamesIgnored") ?: listOf()

    init {
        sync.apply {
            http.connectionTimeout = 750
            http.connectionRetries = false
        }
    }

    override fun check() {
        aem.logger.info("Checking OSGi bundles on $instance")

        val state = sync.osgiFramework.determineBundleState()

        if (state.unknown) {
            statusLogger.error(
                    "Bundles unknown",
                    "Unknown bundle state on $instance"
            )
            return
        }

        val unstable = state.bundlesExcept(symbolicNamesIgnored).filter { !it.stable }
        if (unstable.isNotEmpty()) {
            statusLogger.error(
                    when (unstable.size) {
                        1 -> "Unstable bundle '${unstable.first().symbolicName}'"
                        else -> "Unstable bundles (${Formats.percentExplained(unstable.size, state.bundles.size)})"
                    },
                    "Unstable bundles detected on $instance:\n${unstable.joinToString("\n")}"
            )
        }
    }
}