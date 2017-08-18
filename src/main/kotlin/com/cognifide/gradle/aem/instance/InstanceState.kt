package com.cognifide.gradle.aem.instance

import com.cognifide.gradle.aem.AemConfig
import org.apache.commons.httpclient.params.HttpConnectionParams
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.gradle.api.Project

class InstanceState(val project: Project, val instance: Instance) {

    val config = AemConfig.of(project)

    var bundleStateParametrizer: (HttpConnectionParams) -> Unit = { params ->
        params.connectionTimeout = config.awaitTimeout
        params.soTimeout = config.awaitTimeout
    }

    val bundleState by lazy {
        InstanceSync(project, instance).determineBundleState(bundleStateParametrizer)
    }

    val stable: Boolean
        get() = bundleState.stable

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(instance)
                .append(bundleState)
                .toHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InstanceState

        return EqualsBuilder()
                .append(instance, other.instance)
                .append(bundleState, other.bundleState)
                .isEquals
    }

}