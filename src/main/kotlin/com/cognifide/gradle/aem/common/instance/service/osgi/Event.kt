package com.cognifide.gradle.aem.common.instance.service.osgi

import com.cognifide.gradle.aem.common.instance.Instance
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.common.utils.Patterns
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.time.ZoneId
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class Event {

    @JsonIgnore
    lateinit var instance: Instance

    lateinit var id: String

    lateinit var topic: String

    lateinit var received: String

    @get:JsonIgnore
    val receivedDate: Date get() = instance.date(received.toLong())

    var category: String? = null

    var info: String? = null

    val service: String? get() = info?.let { StringUtils.substringBetween(it, ", objectClass=", ", bundle=") }

    val details: String get() = service?.takeIf { it.isNotBlank() } ?: info?.takeIf { it.isNotBlank() } ?: topic

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Event

        return EqualsBuilder()
            .append(id, other.id)
            .append(topic, other.topic)
            .append(category, other.category)
            .append(received, other.received)
            .isEquals
    }

    override fun hashCode(): Int = HashCodeBuilder()
        .append(id)
        .append(topic)
        .append(category)
        .append(received)
        .toHashCode()

    override fun toString(): String = "Event(details='$details', received='$receivedDate' id='$id', instance='${instance.name}')"
}

fun Sequence<Event>.byTopics(topics: Iterable<String>) = filter { Patterns.wildcard(it.topic, topics) }

fun Sequence<Event>.byAgeMillis(ageMillis: Long, ageZoneId: ZoneId) = filter { Formats.durationFit(it.received.toLong(), ageZoneId, ageMillis) }

fun Sequence<Event>.ignoreDetails(details: Iterable<String>) = filterNot { Patterns.wildcard(it.details, details) }
