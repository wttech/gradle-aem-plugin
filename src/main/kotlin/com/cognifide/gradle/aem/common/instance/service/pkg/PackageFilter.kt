package com.cognifide.gradle.aem.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageFilter private constructor() {

    lateinit var root: String

    lateinit var rules: List<PackageFilterRule>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PackageFilter

        return EqualsBuilder()
                .append(root, other.root)
                .append(rules, other.rules)
                .isEquals
    }

    override fun hashCode() = HashCodeBuilder()
            .append(root)
            .append(rules)
            .toHashCode()

    override fun toString() = "PackageFilter(root='$root', rules=$rules)"
}
