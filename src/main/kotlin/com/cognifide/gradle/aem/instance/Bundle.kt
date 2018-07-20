package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

import org.osgi.framework.Bundle as Base

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
            stateRaw == Base.RESOLVED
        } else {
            stateRaw == Base.ACTIVE
        }

    val state: String
        get() = when (stateRaw) {
            Base.UNINSTALLED -> "uninstalled"
            Base.INSTALLED -> "installed"
            Base.RESOLVED -> "resolved"
            Base.STARTING -> "starting"
            Base.STOPPING -> "stopping"
            Base.ACTIVE -> "active"
            else -> "unknown"
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

    override fun toString(): String {
        return "Bundle(symbolicName='$symbolicName',state='$state',id='$id')"
    }
}