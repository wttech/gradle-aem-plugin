package com.cognifide.gradle.aem.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageFilterRule private constructor() {

    lateinit var modifier: String

    lateinit var pattern: String

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackageFilterRule

        return EqualsBuilder()
                .append(modifier, other.modifier)
                .append(pattern, other.pattern)
                .isEquals
    }

    override fun hashCode() = HashCodeBuilder()
            .append(modifier)
            .append(pattern)
            .toHashCode()
}
