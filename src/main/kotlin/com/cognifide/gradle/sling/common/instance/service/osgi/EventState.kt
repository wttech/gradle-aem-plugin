package com.cognifide.gradle.sling.common.instance.service.osgi

import com.cognifide.gradle.sling.common.instance.Instance
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class EventState private constructor() {

    @JsonIgnore
    lateinit var instance: Instance

    @JsonProperty("data")
    lateinit var events: List<Event>

    lateinit var status: String

    @get:JsonIgnore
    val unknown: Boolean get() = events.isEmpty()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventState

        return EqualsBuilder()
                .append(events, other.events)
                .isEquals
    }

    override fun hashCode(): Int = HashCodeBuilder()
            .append(events)
            .toHashCode()

    override fun toString(): String = "EventState(instance='${instance.name}', status='$status')"

    companion object {
        fun unknown(): EventState = EventState().apply { events = listOf() }
    }
}
