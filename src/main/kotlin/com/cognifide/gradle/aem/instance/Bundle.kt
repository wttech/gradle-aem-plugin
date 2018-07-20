package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class Bundle {
    lateinit var id: String

    lateinit var name: String

    var stateRaw: Int = 0

    lateinit var symbolicName: String

    lateinit var version: String

    var fragment: Boolean = false

    val stable: Boolean
        get() = if (fragment) {
            stateRaw == FRAGMENT_ACTIVE_STATE
        } else {
            stateRaw == BUNDLE_ACTIVE_STATE
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bundle

        return EqualsBuilder()
                .append(id, other.id)
                .append(name, other.name)
                .append(stateRaw, other.stateRaw)
                .append(symbolicName, other.symbolicName)
                .append(version, other.version)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(id)
                .append(name)
                .append(stateRaw)
                .append(symbolicName)
                .append(version)
                .toHashCode()
    }

    companion object {
        const val FRAGMENT_ACTIVE_STATE = 4

        const val BUNDLE_ACTIVE_STATE = 32
    }
}