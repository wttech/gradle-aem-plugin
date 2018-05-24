package com.cognifide.gradle.aem.instance

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class InstanceState(private var _sync: InstanceSync, val instance: Instance) {

    val sync: InstanceSync
        get() = _sync

    val stable: Boolean
        get() = bundleState.stable && !componentState.unknown

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
            it.bundleState.stable
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

    fun checkComponentState(packagesActive: Collection<String>, connectionTimeout: Int = 10000): Boolean {
        return check({
            it.connectionTimeout = connectionTimeout
        }, {
            it.componentState.check(packagesActive, { it.active })
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

}