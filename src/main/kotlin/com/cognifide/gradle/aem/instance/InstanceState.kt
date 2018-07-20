package com.cognifide.gradle.aem.instance

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class InstanceState(private var _sync: InstanceSync, val instance: Instance) {

    val sync: InstanceSync
        get() = _sync

    val status = InstanceStatus()

    val bundleState by lazy { sync.determineBundleState() }

    val componentState by lazy { sync.determineComponentState() }

    /**
     * Customize default synchronization options like basic auth credentials, connection
     * timeouts etc while determining bundle or component states.
     */
    fun <T> check(configurer: (InstanceSync) -> Unit, action: (InstanceState) -> T): T {
        val origin = _sync
        _sync = InstanceSync(_sync.project, _sync.instance).apply(configurer)
        val result = action(this)
        _sync = origin
        return result
    }

    fun checkBundleStable(connectionTimeout: Int = 100): Boolean {
        return check({
            it.connectionTimeout = connectionTimeout
            it.connectionRetries = false
        }, {
            if (it.bundleState.unknown) {
                it.status.error("Unknown bundle state on ${it.instance}")
                return@check false
            }

            val unstableBundles = it.bundleState.bundles.filter { it.stable }
            if (unstableBundles.isNotEmpty()) {
                it.status.error("Unstable bundles detected on ${it.instance}: $unstableBundles")
                return@check false
            }

            return@check true
        })
    }

    fun checkBundleState(connectionTimeout: Int = 100): Int {
        return check({
            it.connectionTimeout = connectionTimeout
            it.connectionRetries = false
        }, {
            it.bundleState.hashCode()
        })
    }

    fun checkComponentState(connectionTimeout: Int = 10000): Boolean {
        return checkComponentState(PLATFORM_COMPONENTS, connectionTimeout)
    }

    fun checkComponentState(packagesActive: Collection<String>, connectionTimeout: Int = 10000): Boolean {
        return check({
            it.connectionTimeout = connectionTimeout
        }, {
            if (it.componentState.unknown) {
                it.status.error("Unknown component state on ${it.instance}")
                return@check false
            }

            val inactiveComponents = it.componentState.find(packagesActive, listOf()).filter { it.active }
            if (inactiveComponents.isNotEmpty()) {
                it.status.error("Inactive components detected on $instance: $inactiveComponents")
                return@check false
            }

            return@check true
        })
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(instance)
                .append(bundleState)
                .append(componentState)
                .toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstanceState

        return EqualsBuilder()
                .append(instance, other.instance)
                .append(bundleState, other.bundleState)
                .append(componentState, other.componentState)
                .isEquals
    }

    companion object {
        val PLATFORM_COMPONENTS = setOf(
                "com.day.crx.packaging.*",
                "org.apache.sling.installer.*"
        )
    }

}