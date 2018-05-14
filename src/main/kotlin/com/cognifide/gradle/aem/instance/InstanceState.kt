package com.cognifide.gradle.aem.instance

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class InstanceState(val sync: InstanceSync, val instance: Instance) {

    val stable: Boolean
        get() = bundleState.stable && componentState.stable

    val bundleState by lazy { sync.determineBundleState() }

    val componentState by lazy { sync.determineComponentState() }

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