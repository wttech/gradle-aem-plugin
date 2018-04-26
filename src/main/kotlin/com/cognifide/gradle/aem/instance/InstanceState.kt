package com.cognifide.gradle.aem.instance

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

class InstanceState(val instance: Instance, val bundleState: BundleState) {

    val stable: Boolean
        get() = bundleState.stable

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(instance)
                .append(bundleState)
                .toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstanceState

        return EqualsBuilder()
                .append(instance, other.instance)
                .append(bundleState, other.bundleState)
                .isEquals
    }

}