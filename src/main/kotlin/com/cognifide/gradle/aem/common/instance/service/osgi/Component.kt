package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class Component {

    lateinit var id: String

    lateinit var name: String

    lateinit var state: String

    var stateRaw: Int = 0

    var bundleId: Int? = -1

    var pid: String? = null

    val uid: String
        get() = pid ?: name

    val active: Boolean
        get() = stateRaw == STATE_RAW_ACTIVE

    val satisfied: Boolean
        get() = stateRaw == STATE_RAW_SATISTIED

    val notUnsatisfied: Boolean
        get() = stateRaw != STATE_RAW_UNSATISFIED

    val unsatisfied: Boolean
        get() = stateRaw == STATE_RAW_UNSATISFIED

    val failedActivation: Boolean
        get() = stateRaw == STATE_RAW_FAILED_ACTIVATION

    val noConfig: Boolean
        get() = state == STATE_NO_CONFIG

    val disabled: Boolean
        get() = state == STATE_DISABLED

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Component

        return EqualsBuilder()
                .append(id, other.id)
                .append(name, other.name)
                .append(state, other.state)
                .append(stateRaw, other.stateRaw)
                .append(pid, other.pid)
                .append(bundleId, other.bundleId)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(id)
                .append(bundleId)
                .append(name)
                .append(stateRaw)
                .append(state)
                .toHashCode()
    }

    override fun toString(): String {
        return "Component(uid='$uid', state='$state', id='$id', bundleId='$bundleId')"
    }

    companion object {
        val STATE_RAW_DISABLED_OR_NO_CONFIG = -1

        val STATE_RAW_UNSATISFIED = 2

        val STATE_RAW_SATISTIED = 4

        val STATE_RAW_ACTIVE = 8

        val STATE_RAW_FAILED_ACTIVATION = 16

        val STATE_ACTIVE = "active"

        val STATE_SATISFIED = "satisfied"

        val STATE_NO_CONFIG = "no config"

        val STATE_DISABLED = "disabled"
    }
}
