package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZoneId
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.tasks.Internal

abstract class AbstractInstance(
    @Transient
    @JsonIgnore
    protected val aem: AemExtension
) : Instance {

    override lateinit var httpUrl: String

    @get:Internal
    override var name: String
        get() = "$environment-$id"
        set(value) {
            environment = value.substringBefore("-")
            id = value.substringAfter("-")
        }

    override lateinit var id: String

    override lateinit var environment: String

    override val sync: InstanceSync
        get() = InstanceSync(aem, this)

    override var properties = mutableMapOf<String, String?>()

    override val systemProperties: Map<String, String>
        get() = sync.status.systemProperties

    override val available: Boolean
        get() = systemProperties.isNotEmpty()

    override fun property(key: String, value: String?) {
        properties[key] = value
    }

    final override fun property(key: String): String? = properties[key] ?: systemProperties[key]

    override val zoneId: ZoneId
        get() = property("user.timezone")?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()

    override val version: String
        get() = sync.status.productVersion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Instance

        return EqualsBuilder()
                .append(name, other.name)
                .append(httpUrl, other.httpUrl)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(name)
                .append(httpUrl)
                .toHashCode()
    }
}