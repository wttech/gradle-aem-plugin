package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.Project

abstract class AbstractInstance(
        @Transient
        @JsonIgnore
        protected val project: Project
) : Instance {

    override val sync: InstanceSync
        get() = InstanceSync(project, this)

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