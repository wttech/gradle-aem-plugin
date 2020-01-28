package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.time.ZoneId

@JsonIgnoreProperties(ignoreUnknown = true)
class EventState private constructor() {

    @JsonProperty("data")
    lateinit var events: List<Event>

    lateinit var status: String

    @get:JsonIgnore
    val unknown: Boolean
        get() = events.isEmpty()

    fun matching(topics: Iterable<String>, ageMillis: Long, ageZoneId: ZoneId): List<Event> = events.filter { event ->
        if (!Patterns.wildcard(event.topic, topics)) {
            return@filter false
        }

        Formats.durationFit(event.received.toLong(), ageZoneId, ageMillis)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventState

        return EqualsBuilder()
                .append(events, other.events)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(events)
                .toHashCode()
    }

    override fun toString(): String {
        return "EventState(status='$status')"
    }

    companion object {

        fun unknown(): EventState = EventState().apply {
            events = listOf()
        }
    }
}
