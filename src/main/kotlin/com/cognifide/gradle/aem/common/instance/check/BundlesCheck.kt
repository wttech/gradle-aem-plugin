package com.cognifide.gradle.aem.common.instance.check

@Suppress("MagicNumber")
class BundlesCheck(group: CheckGroup) : DefaultCheck(group) {

    var symbolicNamesIgnored = listOf<String>()

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
                        1 -> "Bundle unstable '${unstable.first().symbolicName}'"
                        in 2..10 -> "Bundles unstable (${unstable.size})"
                        else -> "Bundles stable (${state.stablePercent})"
                    },
                    "Unstable bundles detected on $instance:\n${unstable.joinToString("\n")}"
            )
        }
    }
}