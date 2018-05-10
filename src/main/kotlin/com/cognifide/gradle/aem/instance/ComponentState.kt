package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.internal.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class ComponentState private constructor() {

    @JsonProperty("data")
    lateinit var components: List<Component>

    @JsonProperty("status")
    var total: Int = 0

    @get:JsonIgnore
    val platformComponents: List<Component>
        get() = components.filter { Patterns.wildcard(it.pid, PLATFORM_COMPONENTS) }

    @get:JsonIgnore
    val unknown: Boolean
        get() = components.isEmpty()

    val stable: Boolean
        get() = !unknown && platformComponents.all { it.stable }

    /**
     * Checks if only components of matching PID pattern are active.
     */
    fun stableOnly(pids: List<String>): Boolean {
        return !unknown && components.filter { Patterns.wildcard(it.pid, pids) }.all { it.stable }
    }

    fun stableOnly(pid: String): Boolean {
        return stableOnly(listOf(pid))
    }

    /**
     * Checks if platform components and these matching PID pattern are active.
     */
    fun stable(pids: List<String>): Boolean {
        return stable && components.filter { Patterns.wildcard(it.pid, pids) }.all { it.stable }
    }

    fun stable(pid: String): Boolean {
        return stable(listOf(pid))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ComponentState

        return EqualsBuilder()
                .append(components, other.components)
                .append(total, other.total)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(components)
                .append(total)
                .toHashCode()
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Component {
        lateinit var id: String

        var bundleId: Int = -1

        lateinit var name: String

        lateinit var state: String

        var stateRaw: Int = 0

        lateinit var pid: String

        val stable: Boolean
            get() = stateRaw != STATE_RAW_UNSATISFIED

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Component

            return EqualsBuilder()
                    .append(id, other.id)
                    .append(bundleId, other.bundleId)
                    .append(name, other.name)
                    .append(state, other.state)
                    .append(stateRaw, other.stateRaw)
                    .append(pid, other.pid)
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

        companion object {
            val STATE_RAW_DISABLED_OR_NO_CONFIG = -1

            val STATE_RAW_UNSATISFIED = 2

            val STATE_RAW_SATISTIED = 4

            val STATE_RAW_ACTIVE = 8

            val STATE_ACTIVE = "active"

            val STATE_SATISFIED = "satisfied"

            val STATE_NO_CONFIG = "no config"

            val STATE_DISABLED = "disabled"
        }
    }

    companion object {

        val PLATFORM_COMPONENTS = listOf(
                "com.day.crx.packaging.*"
        )

        fun fromJson(json: String): ComponentState {
            return ObjectMapper().readValue(json, ComponentState::class.java)
        }

        fun unknown(): ComponentState {
            val response = ComponentState()
            response.components = listOf()
            response.total = 0

            return response
        }
    }

}
