package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.common.AemExtension
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.time.ZoneId

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