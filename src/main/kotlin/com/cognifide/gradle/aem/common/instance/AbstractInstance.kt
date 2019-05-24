package com.cognifide.gradle.aem.common.instance

import com.cognifide.gradle.aem.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.ZoneId
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

abstract class AbstractInstance(
    @Transient
    @JsonIgnore
    protected val aem: AemExtension
) : Instance {

    override var zoneId = ZoneId.systemDefault()

    override var properties = mapOf<String, Any>()

    override fun property(key: String, value: Any) {
        properties += mapOf(key to value)
    }

    override fun property(key: String): Any? = properties[key]

    override fun string(key: String): String? = (properties[key] as String?)?.ifBlank { null }

    override val sync: InstanceSync
        get() = InstanceSync(aem, this)

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