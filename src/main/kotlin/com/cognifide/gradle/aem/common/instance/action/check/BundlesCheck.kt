package com.cognifide.gradle.aem.common.instance.action.check

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.aem.common.instance.action.CheckAction

@Suppress("MagicNumber")
class BundlesCheck(action: CheckAction, instance: Instance) : DefaultCheck(action, instance) {

    init {
        sync.apply {
            http.connectionTimeout = 750
            http.connectionRetries = false
        }
    }

    var symbolicNamesIgnored: Iterable<String> = setOf()

    override fun check() {
        val state = sync.osgiFramework.determineBundleState()

        if (state.unknown) {
            statusLogger.error("Unknown bundle state on $instance")
            return
        }

        val unstableBundles = state.bundlesExcept(symbolicNamesIgnored).filter { !it.stable }
        if (unstableBundles.isNotEmpty()) {
            statusLogger.error("Unstable bundles detected on $instance:\n${unstableBundles.joinToString("\n")}")
        }
    }
}