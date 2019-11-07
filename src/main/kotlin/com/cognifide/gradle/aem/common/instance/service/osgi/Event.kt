package com.cognifide.gradle.aem.common.instance.service.osgi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class Event {

    lateinit var id: String

    lateinit var topic: String

    lateinit var received: String

    var category: String? = null

    var info: String? = null

    val service: String?
        get() = info?.let { StringUtils.substringBetween(it, ", objectClass=", ", bundle=") }

    val details: String
        get() = service?.takeIf { it.isNotBlank() } ?: info?.takeIf { it.isNotBlank() } ?: topic

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

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(id)
                .append(topic)
                .append(category)
                .append(received)
                .toHashCode()
    }

    override fun toString(): String {
        return "Event(details='$details', received='$received' id='$id')"
    }
}
