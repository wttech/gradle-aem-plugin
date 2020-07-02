package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class ComponentState private constructor() {

    @JsonIgnore
    lateinit var instance: Instance

    @JsonProperty("data")
    lateinit var components: List<Component>

    @JsonProperty("status")
    var total: Int = 0

    @get:JsonIgnore
    val unknown: Boolean get() = components.isEmpty()

    fun check(pids: Collection<String>, predicate: (Component) -> Boolean): Boolean {
        return !unknown && find(pids, listOf()).all(predicate)
    }

    fun check(pids: Collection<String>, ignoredPids: Collection<String>, predicate: (Component) -> Boolean): Boolean {
        return !unknown && find(pids, ignoredPids).all(predicate)
    }

    fun find(pids: Collection<String>): List<Component> = find(pids, listOf())

    fun find(pids: Collection<String>, ignoredPids: Collection<String>): List<Component> = components.filter {
        Patterns.wildcard(it.uid, pids) && !Patterns.wildcard(it.uid, ignoredPids)
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

    override fun hashCode(): Int = HashCodeBuilder()
            .append(components)
            .append(total)
            .toHashCode()

    override fun toString(): String = "ComponentState(instance='${instance.name}', total='$total')"

    companion object {
        fun unknown(): ComponentState = ComponentState().apply {
            components = listOf()
            total = 0
        }
    }
}
