package com.cognifide.gradle.aem.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageDependency {

    lateinit var name: String

    lateinit var id: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackageDependency

        return EqualsBuilder()
                .append(name, other.name)
                .append(id, other.id)
                .isEquals
    }

    override fun hashCode() = HashCodeBuilder()
            .append(name)
            .append(id)
            .toHashCode()

    override fun toString() = "PackageDependency(name='$name', id='$id')"
}
