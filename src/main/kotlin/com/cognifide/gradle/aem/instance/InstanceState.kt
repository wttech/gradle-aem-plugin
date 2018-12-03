package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.CollectingLogger
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class InstanceState(private var syncOrigin: InstanceSync, val instance: Instance) {

    val sync: InstanceSync
        get() = syncOrigin

    val status = CollectingLogger()

    val bundleState by lazy { sync.determineBundleState() }

    val componentState by lazy { sync.determineComponentState() }

    /**
     * Customize default synchronization options like basic auth credentials, connection
     * timeouts etc while determining bundle or component states.
     */
    fun <T> check(configurer: InstanceSync.() -> Unit, action: InstanceState.() -> T): T {
        val origin = syncOrigin
        syncOrigin = InstanceSync(syncOrigin.project, syncOrigin.instance).apply(configurer)
        val result = action(this)
        syncOrigin = origin
        return result
    }

    fun checkBundleStable(): Boolean {
        return checkBundleStableExcept(listOf(), BUNDLE_STATE_SYNC_OPTIONS)
    }

    fun checkBundleStableExcept(symbolicNamesIgnored: List<String>): Boolean {
        return checkBundleStableExcept(symbolicNamesIgnored, BUNDLE_STATE_SYNC_OPTIONS)
    }

    fun checkBundleStableExcept(symbolicNamesIgnored: List<String>, syncOptions: InstanceSync.() -> Unit): Boolean {
        return check(syncOptions, {
            if (bundleState.unknown) {
                status.error("Unknown bundle state on $instance")
                return@check false
            }

            val unstableBundles = bundleState.bundlesExcept(symbolicNamesIgnored).filter { !it.stable }
            if (unstableBundles.isNotEmpty()) {
                status.error("Unstable bundles detected on $instance:\n${unstableBundles.joinToString("\n")}")
                return@check false
            }

            return@check true
        })
    }

    fun checkBundleState(): Int {
        return checkBundleState(BUNDLE_STATE_SYNC_OPTIONS)
    }

    fun checkBundleState(syncOptions: InstanceSync.() -> Unit): Int {
        return check(syncOptions, {
            bundleState.hashCode()
        })
    }

    fun checkComponentState(): Boolean {
        return checkComponentState(PLATFORM_COMPONENTS)
    }

    fun checkComponentState(packagesActive: Collection<String>): Boolean {
        return checkComponentState(packagesActive, COMPONENT_STATE_SYNC_OPTIONS)
    }

    fun checkComponentState(packagesActive: Collection<String>, syncOptions: InstanceSync.() -> Unit): Boolean {
        return check(syncOptions, {
            if (componentState.unknown) {
                status.error("Unknown component state on $instance")
                return@check false
            }

            val inactiveComponents = componentState.find(packagesActive, listOf()).filter { !it.active }
            if (inactiveComponents.isNotEmpty()) {
                status.error("Inactive components detected on $instance:\n${inactiveComponents.joinToString("\n")}")
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

        val BUNDLE_STATE_SYNC_OPTIONS: InstanceSync.() -> Unit = {
            this.connectionTimeout = 750
            this.connectionRetries = false
        }

        val COMPONENT_STATE_SYNC_OPTIONS: InstanceSync.() -> Unit = {
            this.connectionTimeout = 10000
        }

        val PLATFORM_COMPONENTS = setOf(
                "com.day.crx.packaging.*",
                "org.apache.sling.installer.*"
        )
    }
}