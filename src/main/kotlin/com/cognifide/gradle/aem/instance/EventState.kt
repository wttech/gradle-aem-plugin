package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.Formats
import com.cognifide.gradle.aem.common.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

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

        val nowTimestamp = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val thenTimestamp = Formats.dateTime(event.received.toLong(), ageZoneId)
        val diffMillis = ChronoUnit.MILLIS.between(thenTimestamp, nowTimestamp)

        diffMillis < ageMillis
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
