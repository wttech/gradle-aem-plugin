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
        syncOrigin = InstanceSync(syncOrigin.aem, syncOrigin.instance).apply(configurer)
        val result = action(this)
        syncOrigin = origin
        return result
    }

    fun checkBundleStable() = checkBundleStableExcept(listOf(), BUNDLE_STATE_SYNC_OPTIONS)

    fun checkBundleStableExcept(
        symbolicNamesIgnored: List<String>,
        syncOptions: InstanceSync.() -> Unit = BUNDLE_STATE_SYNC_OPTIONS
    ): Boolean {
        return check(syncOptions, {
            if (bundleState.unknown) {
                status.error("Unknown bundle state on $instance")
                return@check false
            }

            var result = true

            val unstableBundles = bundleState.bundlesExcept(symbolicNamesIgnored).filter { !it.stable }
            if (unstableBundles.isNotEmpty()) {
                status.error("Unstable bundles detected on $instance:\n${unstableBundles.joinToString("\n")}")
                result = false
            }

            return@check result
        })
    }

    fun checkBundleState(syncOptions: InstanceSync.() -> Unit = BUNDLE_STATE_SYNC_OPTIONS): Int {
        return check(syncOptions, { bundleState.hashCode() })
    }

    fun checkComponentState(
        platformComponents: Collection<String> = PLATFORM_COMPONENTS,
        specificComponents: Collection<String> = listOf(),
        syncOptions: InstanceSync.() -> Unit = COMPONENT_STATE_SYNC_OPTIONS
    ): Boolean {
        return check(syncOptions, {
            if (componentState.unknown) {
                status.error("Unknown component state on $instance")
                return@check false
            }

            var result = true

            val inactiveComponents = componentState.find(platformComponents, listOf()).filter { !it.active }
            if (inactiveComponents.isNotEmpty()) {
                status.error("Inactive components detected on $instance:\n${inactiveComponents.joinToString("\n")}")
                result = false
            }

            val failedComponents = componentState.find(specificComponents, listOf()).filter { it.failedActivation }
            if (failedComponents.isNotEmpty()) {
                status.error("Components with failed activation detected on $instance:\n${failedComponents.joinToString("\n")}")
                result = false
            }

            val unsatisfiedComponents = componentState.find(specificComponents, listOf()).filter { it.unsatisfied }
            if (unsatisfiedComponents.isNotEmpty()) {
                status.error("Unsatisfied components detected on $instance:\n${unsatisfiedComponents.joinToString("\n")}")
                result = false
            }

            return@check result
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