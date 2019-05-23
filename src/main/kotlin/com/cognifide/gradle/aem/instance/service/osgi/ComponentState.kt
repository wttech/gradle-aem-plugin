package com.cognifide.gradle.aem.instance.service.osgi

import com.cognifide.gradle.aem.common.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class ComponentState private constructor() {

    @JsonProperty("data")
    lateinit var components: List<Component>

    @JsonProperty("status")
    var total: Int = 0

    @get:JsonIgnore
    val unknown: Boolean
        get() = components.isEmpty()

    fun check(pids: Collection<String>, predicate: (Component) -> Boolean): Boolean {
        return !unknown && find(pids, listOf()).all(predicate)
    }

    fun check(pids: Collection<String>, ignoredPids: Collection<String>, predicate: (Component) -> Boolean): Boolean {
        return !unknown && find(pids, ignoredPids).all(predicate)
    }

    fun find(pids: Collection<String>): List<Component> {
        return find(pids, listOf())
    }

    fun find(pids: Collection<String>, ignoredPids: Collection<String>): List<Component> {
        return components.filter {
            Patterns.wildcard(it.uid, pids) && !Patterns.wildcard(it.uid, ignoredPids)
        }
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

    override fun toString(): String {
        return "ComponentState(total='$total')"
    }

    companion object {

        fun unknown(): ComponentState = ComponentState().apply {
            components = listOf()
            total = 0
        }
    }
}
