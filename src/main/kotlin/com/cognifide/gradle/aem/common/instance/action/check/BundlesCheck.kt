package com.cognifide.gradle.aem.common.instance.action.check

@Suppress("MagicNumber")
class BundlesCheck(group: CheckGroup) : DefaultCheck(group) {

    init {
        sync.apply {
            http.connectionTimeout = 750
            http.connectionRetries = false
        }
    }

    var symbolicNamesIgnored: Iterable<String> = setOf()

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
                    "Unstable bundles (${unstable.size})",
                    "Unstable bundles detected on $instance:\n${unstable.joinToString("\n")}"
            )
        }
    }
}