package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.deploy.ResponseException
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
            Patterns.wildcard(it.pid, pids) && !Patterns.wildcard(it.pid, ignoredPids)
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

    companion object {

        fun fromJson(json: String): ComponentState {
            return try {
                ObjectMapper().readValue(json, ComponentState::class.java)
            } catch(e: Exception) {
                throw ResponseException("Malformed component state response.")
            }
        }

        fun unknown(): ComponentState {
            val response = ComponentState()
            response.components = listOf()
            response.total = 0

            return response
        }
    }

}
